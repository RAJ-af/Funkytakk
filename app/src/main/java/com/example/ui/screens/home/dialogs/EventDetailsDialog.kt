package com.example.ui.screens.home.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.components.SoftHeadsetIcon
import com.example.ui.theme.LocalFunkyColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.SupabaseEvent
import com.example.ui.theme.GoldAccent

@Composable
fun EventDetailsDialog(
    event: SupabaseEvent,
    onDismiss: () -> Unit,
    onJoinEvent: (SupabaseEvent) -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    val palette = getCategoryPalette(event.category_tag)
    val formattedDate = formatEventDateTime(event.event_time, event.timezone_label)
    val hasRoom = !event.join_room_id.isNullOrBlank()
    val hasUrl = !event.join_url.isNullOrBlank()
    val isActionable = hasRoom || hasUrl

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = 24.dp)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = funkyColors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column {
                    // Header banner image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        palette.darkColor.copy(alpha = 0.85f),
                                        palette.darkColor
                                    )
                                )
                            )
                    ) {
                        if (!event.banner_image_url.isNullOrBlank()) {
                            AsyncImage(
                                model = event.banner_image_url,
                                contentDescription = event.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.6f)
                                            )
                                        )
                                    )
                            )
                        }

                        // Close button in top-right
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(34.dp)
                                .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Category Tag Pill in bottom-start
                        val tagText = event.category_tag?.ifBlank { "Featured" } ?: "Featured"
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(14.dp),
                            shape = RoundedCornerShape(100.dp),
                            color = funkyColors.surface.copy(alpha = 0.95f)
                        ) {
                            Text(
                                text = tagText,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.darkColor
                            )
                        }
                    }

                    // Content details
                    Column(
                        modifier = Modifier.padding(22.dp)
                    ) {
                        // Date/Time
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_ring),
                                contentDescription = null,
                                tint = palette.darkColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formattedDate,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.darkColor,
                                letterSpacing = 0.3.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Title
                        Text(
                            text = event.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = funkyColors.ink,
                            lineHeight = 26.sp
                        )

                        // Description
                        if (!event.description.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = event.description,
                                fontSize = 14.sp,
                                color = funkyColors.mute,
                                lineHeight = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // "Join Event" CTA Button
                        if (isActionable) {
                            Button(
                                onClick = { onJoinEvent(event) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = funkyColors.mango)
                            ) {
                                if (hasRoom) {
                                    SoftHeadsetIcon(
                                        size = 18.dp,
                                        color = funkyColors.onMango
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Join Voice Room",
                                        color = funkyColors.onMango,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_arrow_right),
                                        contentDescription = null,
                                        tint = funkyColors.onMango,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Open Event Link",
                                        color = funkyColors.onMango,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            // Disabled / Information state
                            Button(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    disabledContainerColor = funkyColors.soft
                                )
                            ) {
                                Text(
                                    text = "Event Details (No Link)",
                                    color = funkyColors.mute,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
