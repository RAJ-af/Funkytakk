package com.example.ui.screens.home.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.SupabaseEvent
import com.example.ui.theme.GoldAccent
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.util.*

fun formatEventDateTime(eventTimeIso: String?, timezoneLabel: String?): String {
    if (eventTimeIso.isNullOrBlank()) {
        return if (!timezoneLabel.isNullOrBlank()) timezoneLabel else "Upcoming Event"
    }
    return try {
        val clean = eventTimeIso.trim()
        val parsedDate: Date? = try {
            OffsetDateTime.parse(clean).let { Date.from(it.toInstant()) }
        } catch (_: Exception) {
            try {
                Instant.parse(clean).let { Date.from(it) }
            } catch (_: Exception) {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                sdf.parse(clean.substringBefore("+").substringBefore("Z"))
            }
        }

        if (parsedDate != null) {
            val outFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
            val formatted = outFormat.format(parsedDate)
            if (!timezoneLabel.isNullOrBlank()) {
                "$formatted ($timezoneLabel)"
            } else {
                formatted
            }
        } else {
            if (!timezoneLabel.isNullOrBlank()) "$clean ($timezoneLabel)" else clean
        }
    } catch (_: Exception) {
        if (!timezoneLabel.isNullOrBlank()) "$eventTimeIso ($timezoneLabel)" else eventTimeIso
    }
}

data class EventCategoryPalette(
    val darkColor: Color,
    val lightContainer: Color,
    val tagBackground: Color
)

fun getCategoryPalette(categoryTag: String?): EventCategoryPalette {
    val tag = categoryTag?.lowercase()?.trim() ?: ""
    return when {
        tag.contains("mega") || tag.contains("party") || tag.contains("fiesta") ->
            EventCategoryPalette(Color(0xFFA855F7), Color(0xFFF3E8FF), Color(0xFFFAF5FF))
        tag.contains("culture") || tag.contains("movie") || tag.contains("cinema") ->
            EventCategoryPalette(Color(0xFFEF4444), Color(0xFFFEE2E2), Color(0xFFFFF1F2))
        tag.contains("game") || tag.contains("escape") || tag.contains("trivia") ->
            EventCategoryPalette(Color(0xFF3B82F6), Color(0xFFEFF6FF), Color(0xFFF0F9FF))
        tag.contains("music") || tag.contains("karaoke") || tag.contains("song") ->
            EventCategoryPalette(Color(0xFFEC4899), Color(0xFFFCE7F3), Color(0xFFFDF2F8))
        tag.contains("grammar") || tag.contains("workshop") || tag.contains("learn") ->
            EventCategoryPalette(Color(0xFF10B981), Color(0xFFD1FAE5), Color(0xFFECFDF5))
        tag.contains("vip") || tag.contains("special") ->
            EventCategoryPalette(Color(0xFFD97706), Color(0xFFFEF3C7), Color(0xFFFFFBEB))
        else ->
            EventCategoryPalette(Color(0xFF8B5CF6), Color(0xFFEDE9FE), Color(0xFFF5F3FF))
    }
}

@Composable
fun AmusementEventsDialog(
    events: List<SupabaseEvent>,
    onDismiss: () -> Unit,
    onSelectEvent: (SupabaseEvent) -> Unit,
    onJoinEventDirectly: (SupabaseEvent) -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    val isDark = funkyColors.isDark

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isDark) 0.72f else 0.6f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                colors = CardDefaults.cardColors(containerColor = funkyColors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.88f)
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    // Top Drag Notch
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(funkyColors.line)
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Funky Events",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = funkyColors.ink
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Special language events, mega rooms & live workshops",
                                fontSize = 13.sp,
                                color = funkyColors.mute,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .background(funkyColors.soft, CircleShape)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Close",
                                tint = funkyColors.ink,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Empty State Check
                    if (events.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) funkyColors.soft else Color(0xFFFFFBEB)
                                ),
                                border = BorderStroke(1.dp, if (isDark) funkyColors.line else Color(0xFFFDE68A))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .background(funkyColors.mango.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_sparkle),
                                            contentDescription = null,
                                            tint = funkyColors.mango,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(18.dp))

                                    Text(
                                        text = "No Upcoming Events",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = funkyColors.ink
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "No upcoming events right now — check back soon!",
                                        fontSize = 13.sp,
                                        color = funkyColors.mute,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Button(
                                        onClick = onDismiss,
                                        colors = ButtonDefaults.buttonColors(containerColor = funkyColors.mango),
                                        shape = RoundedCornerShape(100.dp),
                                        modifier = Modifier.height(42.dp)
                                    ) {
                                        Text(
                                            text = "Explore Voice Rooms",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = funkyColors.onMango
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Events List
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(events, key = { it.id ?: it.title }) { event ->
                                val palette = getCategoryPalette(event.category_tag)
                                val formattedDate = formatEventDateTime(event.event_time, event.timezone_label)
                                val hasRoom = !event.join_room_id.isNullOrBlank()
                                val hasUrl = !event.join_url.isNullOrBlank()
                                val isActionable = hasRoom || hasUrl

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectEvent(event) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDark) funkyColors.soft else palette.lightContainer
                                    ),
                                    shape = RoundedCornerShape(22.dp),
                                    border = BorderStroke(1.dp, palette.darkColor.copy(alpha = if (isDark) 0.35f else 0.2f))
                                ) {
                                    Column {
                                        // Event Banner Image with Scrim & Tag
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(135.dp)
                                                .background(
                                                    brush = Brush.verticalGradient(
                                                        colors = listOf(
                                                            palette.darkColor.copy(alpha = 0.8f),
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
                                                                    Color.Black.copy(alpha = 0.65f)
                                                                )
                                                            )
                                                        )
                                                )
                                            }

                                            // Category Tag Pill
                                            val tagText = event.category_tag?.ifBlank { "Featured" } ?: "Featured"
                                            Surface(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(12.dp),
                                                shape = RoundedCornerShape(100.dp),
                                                color = funkyColors.surface.copy(alpha = 0.95f),
                                                shadowElevation = 2.dp
                                            ) {
                                                Text(
                                                    text = tagText,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = palette.darkColor
                                                )
                                            }
                                        }

                                        // Event Content
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            // Date & Time Row
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_ring),
                                                    contentDescription = null,
                                                    tint = palette.darkColor,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = formattedDate,
                                                    color = palette.darkColor,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 12.sp,
                                                    letterSpacing = 0.3.sp
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            // Title
                                            Text(
                                                text = event.title,
                                                color = funkyColors.ink,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 18.sp,
                                                lineHeight = 22.sp
                                            )

                                            // Description
                                            if (!event.description.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = event.description,
                                                    color = funkyColors.mute,
                                                    fontSize = 13.sp,
                                                    lineHeight = 18.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(14.dp))

                                            // Action Button Row
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Tap to view details",
                                                    fontSize = 12.sp,
                                                    color = funkyColors.mute,
                                                    fontWeight = FontWeight.Medium
                                                )

                                                if (isActionable) {
                                                    Button(
                                                        onClick = { onJoinEventDirectly(event) },
                                                        shape = RoundedCornerShape(100.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = funkyColors.mango
                                                        ),
                                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                        modifier = Modifier.height(36.dp)
                                                    ) {
                                                        if (hasRoom) {
                                                            SoftHeadsetIcon(
                                                                size = 14.dp,
                                                                color = funkyColors.onMango
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = "Join Room",
                                                                color = funkyColors.onMango,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 12.sp
                                                            )
                                                        } else {
                                                            Icon(
                                                                painter = painterResource(id = R.drawable.ic_arrow_right),
                                                                contentDescription = null,
                                                                tint = funkyColors.onMango,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = "Open Link",
                                                                color = funkyColors.onMango,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 12.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
