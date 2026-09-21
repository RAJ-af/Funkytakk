package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun ReactionsOverlay(
    activeReactions: List<FloatingEmojiId>,
    onReactionComplete: (FloatingEmojiId) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        activeReactions.forEach { reaction ->
            key(reaction.id) {
                FloatingEmojiAnimation(
                    url = reaction.url,
                    onComplete = { onReactionComplete(reaction) }
                )
            }
        }
    }
}

@Composable
fun FloatingEmojiAnimation(url: String, onComplete: () -> Unit) {
    var isStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isStarted = true
        kotlinx.coroutines.delay(2000)
        onComplete()
    }
    
    val offsetY by animateFloatAsState(
        targetValue = if (isStarted) -600f else 0f, 
        animationSpec = tween(durationMillis = 2000, easing = LinearOutSlowInEasing),
        label = "offsetY"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isStarted) 0f else 1f, 
        animationSpec = tween(durationMillis = 2000, easing = LinearEasing),
        label = "alpha"
    )
    
    val startX = remember { (-80..80).random().dp }
    
    AsyncImage(
        model = url,
        contentDescription = null,
        modifier = Modifier
            .offset(x = startX, y = offsetY.dp)
            .size(48.dp)
            .alpha(alpha)
    )
}
