package com.example.gifting

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Renders gift visual asset:
 * Automatically inspects the URL to detect if it's an MP4 video (animated gift)
 * or a standard image/GIF, rendering either a silent looping video player or an image.
 */
@Composable
fun AnimatedGiftMedia(
    url: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val cleanUrl = url.trim()

    val isVideo = remember(cleanUrl) {
        cleanUrl.endsWith(".mp4", ignoreCase = true) ||
                cleanUrl.contains(".mp4?", ignoreCase = true) ||
                cleanUrl.contains("/video/upload/", ignoreCase = true) ||
                cleanUrl.endsWith(".webm", ignoreCase = true)
    }

    Box(
        modifier = modifier.clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (cleanUrl.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFEF3C7))
            )
        } else if (isVideo) {
            AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(Uri.parse(cleanUrl))
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.setVolume(0f, 0f) // Silent looping gift
                            start()
                        }
                        setOnErrorListener { _, _, _ ->
                            true // Suppress error dialogs
                        }
                    }
                },
                update = { view ->
                    if (!view.isPlaying) {
                        try {
                            view.start()
                        } catch (_: Exception) {}
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(cleanUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
