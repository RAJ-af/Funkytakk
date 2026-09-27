package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object ImageSaveUtils {
    /**
     * Saves an image from any source (HTTP, file, content://, or base64) into the MediaStore Pictures/FunkyTalk collection.
     */
    suspend fun saveImageToGallery(context: Context, imageUrl: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (imageUrl.isBlank()) return@withContext Result.failure(Exception("Image source is empty"))

            val timeStamp = System.currentTimeMillis()
            val filename = "FunkyTalk_$timeStamp.jpg"

            var inputStream: InputStream? = null
            var bitmap: Bitmap? = null

            try {
                when {
                    // Case 1: Base64 data string (e.g. "data:image/jpeg;base64,/9j/4AAQSk...")
                    imageUrl.startsWith("data:image/") || (imageUrl.contains(";base64,") && !imageUrl.startsWith("http")) -> {
                        val base64Data = if (imageUrl.contains(",")) {
                            imageUrl.substringAfter(",")
                        } else {
                            imageUrl
                        }
                        val decodedBytes = Base64.decode(base64Data.trim(), Base64.DEFAULT)
                        inputStream = ByteArrayInputStream(decodedBytes)
                        bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    }

                    // Case 2: Android content:// URI
                    imageUrl.startsWith("content://") -> {
                        val uri = Uri.parse(imageUrl)
                        inputStream = context.contentResolver.openInputStream(uri)
                        bitmap = BitmapFactory.decodeStream(inputStream)
                    }

                    // Case 3: Local file path or file:// URI
                    imageUrl.startsWith("file://") || imageUrl.startsWith("/") -> {
                        val cleanPath = imageUrl.removePrefix("file://")
                        val file = File(cleanPath)
                        if (file.exists()) {
                            bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        } else {
                            return@withContext Result.failure(Exception("Local image file does not exist"))
                        }
                    }

                    // Case 4: Remote HTTP / HTTPS URL
                    imageUrl.startsWith("http://") || imageUrl.startsWith("https://") -> {
                        val url = URL(imageUrl)
                        val conn = url.openConnection() as HttpURLConnection
                        conn.connectTimeout = 15000
                        conn.readTimeout = 20000
                        conn.doInput = true
                        conn.instanceFollowRedirects = true
                        conn.connect()

                        if (conn.responseCode in 200..299) {
                            inputStream = conn.inputStream
                            val bytes = inputStream.readBytes()
                            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        } else {
                            return@withContext Result.failure(Exception("HTTP error ${conn.responseCode} downloading image"))
                        }
                    }

                    else -> {
                        // Attempt fallback decoding assuming raw base64 or file path
                        val file = File(imageUrl)
                        if (file.exists()) {
                            bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        } else {
                            // Try base64
                            try {
                                val decodedBytes = Base64.decode(imageUrl.trim(), Base64.DEFAULT)
                                bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                            } catch (e: Exception) {
                                return@withContext Result.failure(Exception("Unsupported or invalid image format"))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ImageSaveUtils", "Failed to decode stream for $imageUrl", e)
                return@withContext Result.failure(Exception("Failed to decode image: ${e.localizedMessage}"))
            }

            if (bitmap == null) {
                return@withContext Result.failure(Exception("Could not process or decode bitmap"))
            }

            // Save to MediaStore or Public Pictures Directory
            var savedUri: Uri? = null

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/FunkyTalk")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                savedUri = resolver.insert(collection, contentValues)

                savedUri?.let { uri ->
                    resolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 98, out)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, "FunkyTalk")
                if (!appDir.exists()) appDir.mkdirs()
                val file = File(appDir, filename)

                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 98, out)
                }

                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, file.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                }
                savedUri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

                // Trigger MediaScanner broadcast for pre-Q devices
                try {
                    val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                        data = Uri.fromFile(file)
                    }
                    context.sendBroadcast(mediaScanIntent)
                } catch (e: Exception) {
                    Log.w("ImageSaveUtils", "Media scanner broadcast failed", e)
                }
            }

            if (savedUri != null) {
                Result.success("Saved image to Gallery (Pictures/FunkyTalk)")
            } else {
                Result.failure(Exception("Could not save file to MediaStore"))
            }
        } catch (e: Exception) {
            Log.e("ImageSaveUtils", "Error in saveImageToGallery", e)
            Result.failure(e)
        }
    }

    /**
     * Shares image link or text via Android ACTION_SEND chooser.
     */
    fun shareImage(context: Context, imageUrl: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, imageUrl)
            }
            context.startActivity(Intent.createChooser(intent, "Share Image Link"))
        } catch (e: Exception) {
            Log.e("ImageSaveUtils", "Share image failed", e)
        }
    }
}

