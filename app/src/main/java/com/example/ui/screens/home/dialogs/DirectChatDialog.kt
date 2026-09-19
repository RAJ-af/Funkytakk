package com.example.ui.screens.home.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.components.CircleFlag
import com.example.ui.screens.home.models.DirectChat
import com.example.ui.theme.GoldAccent

@Composable
fun DirectChatDialog(
    contact: DirectChat,
    messages: List<Pair<String, Boolean>>,
    onDismiss: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    var messageInputText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Area with contact status banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF9F9FA))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    AsyncImage(
                        model = contact.avatar,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (contact.onlineStatus == "online") Color(0xFF10B981) else Color.Gray)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (contact.onlineStatus == "online") "Active now" else "Offline",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    // Flag of active contact
                    val targetFlag = when (contact.isNativeLanguage) {
                        "English" -> "us"
                        "Korean" -> "kr"
                        "Japanese" -> "jp"
                        "Chinese" -> "cn"
                        else -> "us"
                    }
                    CircleFlag(
                        countryCode = targetFlag,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Message bubbles area
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    reverseLayout = true
                ) {
                    // Show in reversed ordering
                    val reversedMessages = messages.reversed()
                    items(reversedMessages) { (text, isMe) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isMe) 16.dp else 4.dp,
                                            bottomEnd = if (isMe) 4.dp else 16.dp
                                        )
                                    )
                                    .background(if (isMe) GoldAccent else Color(0xFFF3F4F6))
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .widthIn(max = 260.dp)
                            ) {
                                Text(
                                    text = text,
                                    fontSize = 14.sp,
                                    color = Color.Black,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }

                // Bottom chat input row
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f), thickness = 1.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = messageInputText,
                        onValueChange = { messageInputText = it },
                        placeholder = { Text("Write in ${contact.isNativeLanguage}...", fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(100.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (messageInputText.isNotBlank()) {
                                    onSendMessage(messageInputText)
                                    messageInputText = ""
                                }
                            }
                        )
                    )

                    IconButton(
                        onClick = {
                            if (messageInputText.isNotBlank()) {
                                onSendMessage(messageInputText)
                                messageInputText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(GoldAccent)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
