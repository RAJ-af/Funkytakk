package com.example

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import com.example.service.PresenceManager

class FunkyTalkApplication : Application(), ImageLoaderFactory {
    companion object {
        lateinit var appContext: android.content.Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        PresenceManager.initialize(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
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
