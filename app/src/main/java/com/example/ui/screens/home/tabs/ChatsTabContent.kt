package com.example.ui.screens.home.tabs

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.*
import com.example.ui.screens.home.models.DirectChat

@Composable
fun ChatsTabContent(
    conversationsList: List<DirectChat>,
    onSelectChat: (DirectChat) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Unread", "Language", "Groups"
    var showOptionsMenu by remember { mutableStateOf(false) }

    // Dynamic list that can be updated (e.g., mark all as read)
    var localConversations by remember(conversationsList) {
        mutableStateOf(conversationsList)
    }

    val filteredList = remember(localConversations, searchQuery, selectedFilter) {
        localConversations.filter { chat ->
            val matchesSearch = searchQuery.isBlank() ||
                    chat.name.contains(searchQuery, ignoreCase = true) ||
                    chat.lastMsg.contains(searchQuery, ignoreCase = true) ||
                    chat.isNativeLanguage.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "Unread" -> chat.unreadCount > 0
                "Language" -> true
                "Groups" -> false
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCFBF7)),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        // 1. Top App Header with Brand Name and 3-dots Menu Button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // FunkyTalk. Brand Title with signature yellow accent
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                color = Color.Black,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 25.sp
                            )
                        ) {
                            append("Funky")
                        }
                        withStyle(
                            style = SpanStyle(
                                color = Color(0xFFFFC529),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 25.sp
                            )
                        ) {
                            append("Talk.")
                        }
                    }
                )

                // Top Right: Functional 3-Dots Action Button with Dropdown Menu
                Box {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F4F6))
                            .clickable { showOptionsMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        FunkyMoreIcon(color = Color.Black, modifier = Modifier.size(18.dp))
                    }

                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mark all as read", color = Color.Black, fontSize = 14.sp) },
                            onClick = {
                                localConversations = localConversations.map { it.copy(unreadCount = 0) }
                                showOptionsMenu = false
                                Toast.makeText(context, "All conversations marked as read", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("New conversation", color = Color.Black, fontSize = 14.sp) },
                            onClick = {
                                showOptionsMenu = false
                                if (localConversations.isNotEmpty()) {
                                    onSelectChat(localConversations.first())
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Chat settings", color = Color.Black, fontSize = 14.sp) },
                            onClick = {
                                showOptionsMenu = false
                                Toast.makeText(context, "Opening chat settings...", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }

        // 2. Headline: Conversations & Subtitle with Gold Heart
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "Conversations",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Keep the conversations going ",
                        fontSize = 15.sp,
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Normal
                    )
                    FunkyHeartIcon(
                        color = Color(0xFFFFC529),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // 3. Search Bar
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color.White)
                    .border(BorderStroke(1.dp, Color(0xFFE5E7EB)), RoundedCornerShape(50.dp))
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FunkySearchIcon(
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search conversations...",
                                color = Color(0xFF9CA3AF),
                                fontSize = 15.sp
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 4. Filter Chips Row ("All", "Unread", "Language", "Groups")
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("All", "Unread", "Language", "Groups").forEach { chipText ->
                    val isSelected = selectedFilter == chipText
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(
                                if (isSelected) Color(0xFFFFC529) else Color(0xFFF3F4F6)
                            )
                            .clickable { selectedFilter = chipText }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chipText,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else Color(0xFF1F2937)
                        )
                    }
                }
            }
        }

        // 5. Chat Cards List
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        items(filteredList, key = { it.id }) { chat ->
            val flagCode = when (chat.isNativeLanguage.lowercase()) {
                "english" -> "us"
                "korean" -> "kr"
                "japanese" -> "jp"
                "chinese" -> "cn"
                "spanish" -> "es"
                "german" -> "de"
                "french" -> "fr"
                "hindi" -> "in"
                "russian" -> "ru"
                "arabic" -> "ae"
                else -> "us"
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 5.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onSelectChat(chat) },
                shape = RoundedCornerShape(22.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF0F0EE)),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Avatar with Status Dot
                    Box(
                        modifier = Modifier.size(56.dp)
                    ) {
                        AsyncImage(
                            model = chat.avatar,
                            contentDescription = chat.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )

                        // Circular Country Flag icon on bottom right replacing the green dot
                        CircleFlag(
                            countryCode = flagCode,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .border(1.5.dp, Color.White, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Center Details
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Top line: Name & Timestamp
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = chat.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )

                            Text(
                                text = chat.time,
                                fontSize = 12.sp,
                                color = Color(0xFF9CA3AF),
                                fontWeight = FontWeight.Normal
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Middle line: Flag + Language Name
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircleFlag(
                                countryCode = flagCode,
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = chat.isNativeLanguage,
                                color = Color(0xFF6B7280),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Bottom line: Last Message & Unread Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = chat.lastMsg,
                                color = if (chat.unreadCount > 0) Color(0xFF1F2937) else Color(0xFF6B7280),
                                fontWeight = if (chat.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            if (chat.unreadCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFC529)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${chat.unreadCount}",
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
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
