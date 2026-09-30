package com.example

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import com.example.service.PresenceManager
import okhttp3.OkHttpClient

class FunkyTalkApplication : Application(), ImageLoaderFactory {
    companion object {
        lateinit var appContext: android.content.Context
            private set
        const val APPWRITE_PROJECT_ID = "6abbbb060037fb0d8b82"
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
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
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
                add(SvgDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }
}
