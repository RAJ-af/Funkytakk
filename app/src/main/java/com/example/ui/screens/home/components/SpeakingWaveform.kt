package com.example.ui.screens.home.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SpeakingWaveform(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "waveform")
    
    // Animate heights of the three bars with offset phases to make them look organic
    val bar1Height by transition.animateFloat(
        initialValue = 5f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2Height by transition.animateFloat(
        initialValue = 16f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3Height by transition.animateFloat(
        initialValue = 8f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(bar1Height.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color.White)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(bar2Height.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color.White)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(bar3Height.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color.White)
        )
    }
}
