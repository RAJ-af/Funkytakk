package com.example.ui.chat

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import android.media.ExifInterface
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * Google Play-compliant chat media saving, sharing, and image preparation utilities.
 * Uses MediaStore on Android 10+ with zero broad storage permissions required.
 */
object ChatMediaSaver {
    private const val TAG = "ChatMediaSaver"

    /**
     * Resolves an image source (local path, file:// URI, content:// URI, or HTTP URL) into JPEG/raw bytes
     * and saves it to Pictures/FunkyTalk in the system gallery via MediaStore.
     */
    suspend fun saveImageToGallery(
        context: Context,
        imageSource: String,
        displayNamePrefix: String = "FunkyTalk"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val clean = imageSource.trim()
            if (clean.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Image not available"))
            }

            val bytes = readSourceBytes(context, clean)
                ?: return@withContext Result.failure(IllegalStateException("Image is still downloading or unavailable"))

            if (bytes.isEmpty()) {
                return@withContext Result.failure(IllegalStateException("Empty image file"))
            }

            val isPng = bytes.size >= 8 &&
                bytes[0] == 0x89.toByte() &&
                bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() &&
                bytes[3] == 0x47.toByte()
            val mimeType = if (isPng) "image/png" else "image/jpeg"
            val ext = if (isPng) "png" else "jpg"
            val fileName = "${displayNamePrefix}_${System.currentTimeMillis()}.$ext"

            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/FunkyTalk")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext Result.failure(IllegalStateException("Could not create gallery entry"))

            try {
                resolver.openOutputStream(uri)?.use { out ->
                    out.write(bytes)
                    out.flush()
                } ?: return@withContext Result.failure(IllegalStateException("Could not write to gallery"))

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val publish = ContentValues().apply {
                        put(MediaStore.Images.Media.IS_PENDING, 0)
                    }
                    resolver.update(uri, publish, null, null)
                }
                Result.success(uri)
            } catch (e: Exception) {
                runCatching { resolver.delete(uri, null, null) }
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "saveImageToGallery failed", e)
            Result.failure(e)
        }
    }

    /**
     * Shares a chat image cleanly via FileProvider into an external share chooser.
     */
    suspend fun shareImage(
        context: Context,
        imageSource: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bytes = readSourceBytes(context, imageSource.trim())
                ?: return@withContext Result.failure(IllegalStateException("Image unavailable"))
            val shareDir = File(context.cacheDir, "shared_media").apply { mkdirs() }
            val tempFile = File(shareDir, "share_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use {
                it.write(bytes)
                it.flush()
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )

            withContext(Dispatchers.Main) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(intent, "Share photo").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "shareImage failed", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Could not share image", Toast.LENGTH_SHORT).show()
            }
            Result.failure(e)
        }
    }

    /**
     * Saves a local/remote chat video into Movies/FunkyTalk via MediaStore (zero broad permissions on API 29+).
     */
    suspend fun saveVideoToGallery(
        context: Context,
        videoSource: String,
        displayNamePrefix: String = "FunkyTalk"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val clean = videoSource.trim()
            if (clean.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Video not available"))
            }
            val fileName = "${displayNamePrefix}_${System.currentTimeMillis()}.mp4"
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/FunkyTalk")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext Result.failure(IllegalStateException("Could not create gallery entry"))

            try {
                resolver.openOutputStream(uri)?.use { out ->
                    val copied = copySourceToStream(context, clean, out)
                    if (!copied) {
                        throw IllegalStateException("Video is still downloading or unavailable")
                    }
                    out.flush()
                } ?: return@withContext Result.failure(IllegalStateException("Could not write to gallery"))

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val publish = ContentValues().apply {
                        put(MediaStore.Video.Media.IS_PENDING, 0)
                    }
                    resolver.update(uri, publish, null, null)
                }
                Result.success(uri)
            } catch (e: Exception) {
                runCatching { resolver.delete(uri, null, null) }
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "saveVideoToGallery failed", e)
            Result.failure(e)
        }
    }

    /**
     * Shares any chat media file (image, video, audio, or voice note) via FileProvider.
     */
    suspend fun shareMediaFile(
        context: Context,
        source: String,
        mimeType: String = "application/octet-stream",
        chooserTitle: String = "Share media"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val clean = source.trim()
            val ext = when {
                mimeType.startsWith("video/") -> "mp4"
                mimeType.contains("mpeg") || mimeType.contains("mp3") -> "mp3"
                mimeType.contains("ogg") -> "ogg"
                mimeType.startsWith("audio/") -> "m4a"
                mimeType.contains("png") -> "png"
                else -> "jpg"
            }
            val shareDir = File(context.cacheDir, "shared_media").apply { mkdirs() }
            val tempFile = File(shareDir, "share_${System.currentTimeMillis()}.$ext")
            FileOutputStream(tempFile).use { out ->
                val ok = copySourceToStream(context, clean, out)
                if (!ok) throw IllegalStateException("Media unavailable")
                out.flush()
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )

            withContext(Dispatchers.Main) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(intent, chooserTitle).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "shareMediaFile failed", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Could not share media", Toast.LENGTH_SHORT).show()
            }
            Result.failure(e)
        }
    }

    private fun copySourceToStream(context: Context, source: String, out: java.io.OutputStream): Boolean {
        if (source.startsWith("file://")) {
            val path = Uri.parse(source).path ?: source.removePrefix("file://")
            val file = File(path)
            if (file.exists() && file.length() > 0L) {
                file.inputStream().use { it.copyTo(out) }
                return true
            }
        }
        if (source.startsWith("content://")) {
            context.contentResolver.openInputStream(Uri.parse(source))?.use {
                it.copyTo(out)
                return true
            }
        }
        if (source.startsWith("/")) {
            val file = File(source)
            if (file.exists() && file.length() > 0L) {
                file.inputStream().use { it.copyTo(out) }
                return true
            }
        }
        if (source.startsWith("http://") || source.startsWith("https://")) {
            val conn = URL(source).openConnection().apply {
                connectTimeout = 10_000
                readTimeout = 20_000
            }
            conn.getInputStream().use {
                it.copyTo(out)
                return true
            }
        }
        return false
    }

    /**
     * Reads, EXIF-orients, optionally rotates, and scales/compresses a selected photo before E2EE upload.
     * Supports Standard (1440px, 82% JPEG) and HD (2560px, 92% JPEG).
     */
    suspend fun prepareOutgoingImageBytes(
        context: Context,
        uri: Uri,
        extraRotationDegrees: Int = 0,
        highQuality: Boolean = false
    ): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            var exifOrientation = ExifInterface.ORIENTATION_NORMAL
            resolver.openInputStream(uri)?.use { stream ->
                runCatching {
                    val exif = ExifInterface(stream)
                    exifOrientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                }
            }

            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOpts) }
            val maxDim = if (highQuality) 2560 else 1440
            var sampleSize = 1
            while ((boundsOpts.outWidth / sampleSize) > maxDim * 2 ||
                (boundsOpts.outHeight / sampleSize) > maxDim * 2
            ) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
                ?: return@withContext resolver.openInputStream(uri)?.use { it.readBytes() }

            val exifRotation = when (exifOrientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
            val totalRotation = ((exifRotation + extraRotationDegrees) % 360 + 360) % 360

            val rotated = if (totalRotation != 0) {
                val m = Matrix().apply { postRotate(totalRotation.toFloat()) }
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true).also {
                    if (it !== decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            val finalBitmap = if (rotated.width > maxDim || rotated.height > maxDim) {
                val ratio = minOf(maxDim.toFloat() / rotated.width, maxDim.toFloat() / rotated.height)
                val targetW = (rotated.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (rotated.height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(rotated, targetW, targetH, true).also {
                    if (it !== rotated) rotated.recycle()
                }
            } else {
                rotated
            }

            val quality = if (highQuality) 92 else 82
            val out = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            finalBitmap.recycle()
            out.toByteArray()
        } catch (e: Exception) {
            Log.e(TAG, "prepareOutgoingImageBytes failed, falling back to raw stream", e)
            runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        }
    }

    private fun readSourceBytes(context: Context, source: String): ByteArray? {
        if (source.startsWith("file://")) {
            val path = Uri.parse(source).path ?: source.removePrefix("file://")
            val file = File(path)
            if (file.exists() && file.length() > 0L) return file.readBytes()
        }
        if (source.startsWith("content://")) {
            return context.contentResolver.openInputStream(Uri.parse(source))?.use { it.readBytes() }
        }
        if (source.startsWith("/")) {
            val file = File(source)
            if (file.exists() && file.length() > 0L) return file.readBytes()
        }
        if (source.startsWith("http://") || source.startsWith("https://")) {
            val conn = URL(source).openConnection().apply {
                connectTimeout = 10_000
                readTimeout = 15_000
            }
            return conn.getInputStream().use { it.readBytes() }
        }
        return null
    }
}
