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
import okhttp3.OkHttpClient

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
        super.onCreate()
        _instance = this
        appContext = applicationContext
        PushNotificationManager.initialize(this)
        PresenceManager.initialize(this)
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                if (request.url.host.contains("appwrite.io")) {
                    val newRequest = request.newBuilder()
                        .header("X-Appwrite-Project", APPWRITE_PROJECT_ID)
                        .build()
                    chain.proceed(newRequest)
                } else {
                    chain.proceed(request)
                }
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
}
