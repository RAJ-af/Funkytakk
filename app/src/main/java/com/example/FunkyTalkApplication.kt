package com.example

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.service.PresenceManager
import com.example.service.PushNotificationManager
import com.example.util.StartupTimingTracker
import com.google.firebase.FirebaseApp
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class FunkyTalkApplication : Application(), ImageLoaderFactory {
    companion object {
        private var _instance: FunkyTalkApplication? = null
        val instance: FunkyTalkApplication
            get() = _instance ?: throw IllegalStateException("FunkyTalkApplication is not yet initialized")

        lateinit var appContext: android.content.Context
            private set

        fun getSafeAppContext(): android.content.Context {
            return _instance?.applicationContext
                ?: (if (Companion::appContext.isInitialized) appContext else null)
                ?: throw IllegalStateException("Application context is null - FunkyTalkApplication not initialized")
        }

        const val APPWRITE_PROJECT_ID = "6abbbb060037fb0d8b82"
    }

    override fun onCreate() {
        StartupTimingTracker.markAppCreate()
        super.onCreate()
        _instance = this
        appContext = applicationContext
        com.example.diagnostics.CrashAndErrorReporter.install(this)
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            AppCheckInstaller.install(this)
        } catch (_: Exception) {
        }
        LegacySecurityMigration.runIfNeeded(this)
        AppFlagsRepository.init(this)
        AppFlagsRepository.refreshAsync(this)
        com.example.ui.theme.AppThemeManager.init(this)
        TelemetryManager.init(this)
        PushNotificationManager.initialize(this)
        PresenceManager.initialize(this)
        com.example.crypto.ChatLockManager.attachLifecycleObserver(this)
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val rawUrl = request.url.toString()
                val optimizedUrl = optimizeCloudinaryImageUrl(rawUrl)
                val builder = request.newBuilder()
                if (optimizedUrl != rawUrl) {
                    builder.url(optimizedUrl)
                }
                if (request.url.host.contains("appwrite.io")) {
                    builder.header("X-Appwrite-Project", APPWRITE_PROJECT_ID)
                }
                chain.proceed(builder.build())
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(150L * 1024 * 1024) // 150 MB
                    .build()
            }
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
            }
            .crossfade(150)
            .build()
    }

    companion object Helpers {
        internal fun optimizeCloudinaryImageUrl(url: String): String {
            if (!url.contains("res.cloudinary.com/") || !url.contains("/image/upload/")) {
                return url
            }
            val afterUpload = url.substringAfter("/image/upload/")
            val firstSegment = afterUpload.substringBefore('/')
            // Only inject transformation if no transformation flags are already present
            val looksLikeVersionOrFolder = firstSegment.startsWith("v") && firstSegment.drop(1).all { it.isDigit() }
            if (!looksLikeVersionOrFolder && firstSegment.contains(",")) {
                return url
            }
            if (afterUpload.contains("f_auto") || afterUpload.contains("q_auto")) {
                return url
            }
            return url.replaceFirst(
                "/image/upload/",
                "/image/upload/f_auto,q_auto,w_900,c_limit/"
            )
        }
    }
}
