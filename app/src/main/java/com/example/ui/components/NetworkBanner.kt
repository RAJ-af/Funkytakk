package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ConnectivityBannerState {
    Hidden,
    Offline,
    BackOnline
}

/**
 * Slim, non-blocking top banner reacting to global ConnectivityManager callbacks.
 * Shows "No internet connection" while offline and "Back online" for 2s on recovery.
 */
@Composable
fun GlobalConnectivityBanner(
    modifier: Modifier = Modifier
) {
    val bannerState by AppNetworkManager.bannerState.collectAsState()

    val bgColor by animateColorAsState(
        targetValue = when (bannerState) {
            ConnectivityBannerState.Offline -> Color(0xFFDC2626)
            ConnectivityBannerState.BackOnline -> Color(0xFF16A34A)
            ConnectivityBannerState.Hidden -> Color.Transparent
        },
        label = "connectivity_banner_color"
    )

    AnimatedVisibility(
        visible = bannerState != ConnectivityBannerState.Hidden,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .testTag("global_connectivity_banner")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(Color.White, CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (bannerState == ConnectivityBannerState.BackOnline) {
                        "Back online"
                    } else {
                        "No internet connection"
                    },
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
