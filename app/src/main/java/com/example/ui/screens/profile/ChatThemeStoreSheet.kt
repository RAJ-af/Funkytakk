package com.example.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ChatPreferencesManager
import com.example.ui.chat.ChatPreset
import com.example.ui.chat.chatColors

data class ThemeStoreItem(
    val preset: ChatPreset,
    val title: String,
    val description: String,
    val emoji: String,
    val previewBg: Color,
    val previewOutBubble: Color,
    val previewInBubble: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThemeStoreSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentPreset by ChatPreferencesManager.chatPreset.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val themeList = remember {
        listOf(
            ThemeStoreItem(
                preset = ChatPreset.Butter,
                title = "Butter Gold",
                description = "Warm amber & soft gold bubbles",
                emoji = "💛",
                previewBg = Color(0xFFF6F4EE),
                previewOutBubble = Color(0xFFFFE8A3),
                previewInBubble = Color(0xFFFFFFFF)
            ),
            ThemeStoreItem(
                preset = ChatPreset.Sky,
                title = "Sky Blue",
                description = "Ocean breeze blue & sky background",
                emoji = "🌊",
                previewBg = Color(0xFFEEF3F8),
                previewOutBubble = Color(0xFFD6E8FF),
                previewInBubble = Color(0xFFFFFFFF)
            ),
            ThemeStoreItem(
                preset = ChatPreset.Mint,
                title = "Mint Fresh",
                description = "Spring green & fresh mint bubbles",
                emoji = "🌱",
                previewBg = Color(0xFFEEF4EF),
                previewOutBubble = Color(0xFFD5F2E3),
                previewInBubble = Color(0xFFFFFFFF)
            ),
            ThemeStoreItem(
                preset = ChatPreset.SunsetGlow,
                title = "Sunset Glow",
                description = "Warm coral pink & peach sunset vibes",
                emoji = "🌅",
                previewBg = Color(0xFFFFF4F1),
                previewOutBubble = Color(0xFFFFD4C1),
                previewInBubble = Color(0xFFFFFFFF)
            ),
            ThemeStoreItem(
                preset = ChatPreset.LavenderBliss,
                title = "Lavender Bliss",
                description = "Soft pastel violet & lavender dream",
                emoji = "🔮",
                previewBg = Color(0xFFF6F0FF),
                previewOutBubble = Color(0xFFE4D5FF),
                previewInBubble = Color(0xFFFFFFFF)
            ),
            ThemeStoreItem(
                preset = ChatPreset.RoseVelvet,
                title = "Rose Velvet",
                description = "Elegant blush pink & rose velvet",
                emoji = "🌹",
                previewBg = Color(0xFFFFF0F5),
                previewOutBubble = Color(0xFFFBCFE8),
                previewInBubble = Color(0xFFFFFFFF)
            ),
            ThemeStoreItem(
                preset = ChatPreset.TelegramBlue,
                title = "Telegram Classic",
                description = "Classic Telegram sky blue bubbles",
                emoji = "✈️",
                previewBg = Color(0xFFEDF4FA),
                previewOutBubble = Color(0xFFEFF7FF),
                previewInBubble = Color(0xFFFFFFFF)
            ),
            ThemeStoreItem(
                preset = ChatPreset.SignalEmerald,
                title = "Signal Emerald",
                description = "Clean emerald green & slate background",
                emoji = "🛡️",
                previewBg = Color(0xFFE8F5E9),
                previewOutBubble = Color(0xFFC8E6C9),
                previewInBubble = Color(0xFFFFFFFF)
            ),
            ThemeStoreItem(
                preset = ChatPreset.Graphite,
                title = "Graphite Charcoal",
                description = "Sleek slate graphite & neutral grey",
                emoji = "⚡",
                previewBg = Color(0xFFF2F3F5),
                previewOutBubble = Color(0xFFE3E5E8),
                previewInBubble = Color(0xFFFFFFFF)
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFFAFAF9),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🎨 Chat Theme & Bubble Store",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Choose a bubble theme to personalize all your chats",
                        fontSize = 12.5.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // List of Themes
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(themeList, key = { it.preset.name }) { item ->
                    val isSelected = currentPreset == item.preset

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                ChatPreferencesManager.setChatPreset(context, item.preset)
                                Toast.makeText(context, "Applied ${item.title} theme!", Toast.LENGTH_SHORT).show()
                            },
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFFFFC629) else Color(0xFFE2E8F0)
                        ),
                        shadowElevation = if (isSelected) 3.dp else 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(item.emoji, fontSize = 22.sp)
                                    Column {
                                        Text(
                                            text = item.title,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = item.description,
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = Color(0xFFFEF3C7),
                                        border = BorderStroke(1.dp, Color(0xFFFDE047))
                                    ) {
                                        Text(
                                            text = "Active",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Mini Live Chat Preview
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(84.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(item.previewBg)
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Incoming message preview
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(item.previewInBubble)
                                                .border(0.5.dp, Color(0x18000000), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "Hey! How do you like this theme? ✨",
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF1B1B1B)
                                            )
                                        }
                                    }

                                    // Outgoing message preview
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(item.previewOutBubble)
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "Looks super clean and beautiful! ❤️",
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF1B1B1B)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFC629),
                    contentColor = Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text("Done", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
