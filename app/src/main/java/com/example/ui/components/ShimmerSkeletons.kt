package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LocalFunkyColors

@Composable
fun rememberShimmerBrush(): Brush {
    val funkyColors = LocalFunkyColors.current
    val base = funkyColors.soft
    val highlight = if (funkyColors.isDark) {
        funkyColors.line.copy(alpha = 0.7f)
    } else {
        funkyColors.surface
    }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(translate - 320f, translate - 320f),
        end = Offset(translate, translate)
    )
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(rememberShimmerBrush())
    )
}

@Composable
fun HomeFeedSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            repeat(5) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ShimmerBox(modifier = Modifier.size(56.dp), shape = CircleShape)
                    Spacer(modifier = Modifier.height(6.dp))
                    ShimmerBox(modifier = Modifier.width(46.dp).height(10.dp), shape = RoundedCornerShape(6.dp))
                }
            }
        }
        repeat(3) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(204.dp),
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
fun DiscoverSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(4) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp),
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
fun MomentsSkeleton(modifier: Modifier = Modifier) {
    val funkyColors = LocalFunkyColors.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(funkyColors.bg)
            .padding(horizontal = 20.dp, vertical = 88.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ShimmerBox(modifier = Modifier.size(44.dp), shape = CircleShape)
                Spacer(modifier = Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShimmerBox(modifier = Modifier.width(130.dp).height(14.dp))
                    ShimmerBox(modifier = Modifier.width(86.dp).height(10.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(260.dp), shape = RoundedCornerShape(24.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.85f).height(18.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f).height(14.dp))
        }
    }
}

@Composable
fun ProfileSkeleton(modifier: Modifier = Modifier) {
    val funkyColors = LocalFunkyColors.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(funkyColors.bg)
    ) {
        ShimmerBox(modifier = Modifier.fillMaxWidth().height(185.dp), shape = RoundedCornerShape(0.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBox(modifier = Modifier.size(84.dp), shape = CircleShape)
            ShimmerBox(modifier = Modifier.width(170.dp).height(24.dp))
            ShimmerBox(modifier = Modifier.width(110.dp).height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(3) {
                    ShimmerBox(modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(14.dp))
                }
            }
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(112.dp), shape = RoundedCornerShape(18.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(92.dp), shape = RoundedCornerShape(18.dp))
        }
    }
}

@Composable
fun FollowersSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(4) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(modifier = Modifier.size(46.dp), shape = CircleShape)
                Spacer(modifier = Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShimmerBox(modifier = Modifier.width(130.dp).height(14.dp))
                    ShimmerBox(modifier = Modifier.width(84.dp).height(11.dp))
                }
            }
        }
    }
}

@Composable
fun FriendlyErrorRetryCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    isOnline: Boolean = true,
    retryLabel: String = "Retry",
    testTagPrefix: String = "screen"
) {
    val funkyColors = LocalFunkyColors.current
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = funkyColors.surface,
        border = BorderStroke(1.dp, funkyColors.line),
        modifier = modifier
            .fillMaxWidth()
            .testTag("${testTagPrefix}_error_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(funkyColors.soft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_alert),
                    contentDescription = "Error",
                    tint = funkyColors.mango,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = message,
                color = funkyColors.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            if (!isOnline) {
                Text(
                    text = "Connect to the internet to refresh.",
                    color = funkyColors.mute,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
            Button(
                onClick = onRetry,
                enabled = isOnline,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = funkyColors.mango,
                    contentColor = funkyColors.onMango,
                    disabledContainerColor = funkyColors.soft,
                    disabledContentColor = funkyColors.mute
                ),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("${testTagPrefix}_retry_button")
            ) {
                Text(
                    text = retryLabel,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
