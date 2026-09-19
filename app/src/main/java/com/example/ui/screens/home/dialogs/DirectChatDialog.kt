package com.example.ui.screens.home.dialogs

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.*
import com.example.ui.screens.home.models.DirectChat
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ChatMsgType {
    TEXT,
    AUDIO,
    IMAGE
}

data class RichChatMessage(
    val id: String,
    val text: String = "",
    val translation: String? = null,
    val isTranslated: Boolean = false,
    val isMe: Boolean = false,
    val time: String = "10:15 AM",
    val type: ChatMsgType = ChatMsgType.TEXT,
    val audioDuration: String = "0:12",
    val imageUrl: String? = null,
    val reactions: List<String> = emptyList(),
    val replyToText: String? = null,
    val replyToSender: String? = null
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DirectChatDialog(
    contact: DirectChat,
    messages: List<Pair<String, Boolean>>,
    onDismiss: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    BackHandler {
        onDismiss()
    }

    var messageInputText by remember { mutableStateOf("") }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var isVoicePlaying by remember { mutableStateOf(false) }
    var showQuickReplies by remember { mutableStateOf(false) }

    // Contextual Long-press state
    var selectedMessageForMenu by remember { mutableStateOf<RichChatMessage?>(null) }
    var showMoreSubmenu by remember { mutableStateOf(false) }

    // Active Reply State
    var replyingToMessage by remember { mutableStateOf<RichChatMessage?>(null) }

    // Active Edit State
    var editingMessageId by remember { mutableStateOf<String?>(null) }

    // Map flag and country name
    val (flagCode, countryName) = when (contact.isNativeLanguage.lowercase()) {
        "english" -> "us" to "USA"
        "korean" -> "kr" to "Korea"
        "japanese" -> "jp" to "Japan"
        "chinese" -> "cn" to "China"
        "spanish" -> "es" to "Spain"
        "german" -> "de" to "Germany"
        "french" -> "fr" to "France"
        "hindi" -> "in" to "India"
        "russian" -> "ru" to "Russia"
        "arabic" -> "ae" to "UAE"
        else -> "us" to "Global"
    }

    // Initialize rich message list customized per contact matching user references
    val chatMessages = remember(contact.id) {
        mutableStateListOf<RichChatMessage>().apply {
            when {
                contact.name.contains("Chen", ignoreCase = true) || contact.id == "c_4" -> {
                    add(
                        RichChatMessage(
                            id = "1",
                            text = "Hello! Glad we connected here. 😊",
                            translation = "नमस्ते! मुझे खुशी है कि हम यहाँ जुड़े। 😊",
                            isMe = false,
                            time = "10:15 AM"
                        )
                    )
                    add(
                        RichChatMessage(
                            id = "2",
                            text = "Thanks for helping me with English yesterday!",
                            translation = "कल अंग्रेजी में मेरी मदद करने के लिए धन्यवाद!",
                            isMe = false,
                            time = "10:16 AM"
                        )
                    )
                    add(
                        RichChatMessage(
                            id = "3",
                            text = "Hey! Thanks for connecting, let's practice together!",
                            isMe = true,
                            time = "10:18 AM"
                        )
                    )
                }
                contact.name.contains("Sarah", ignoreCase = true) || contact.id == "c_1" -> {
                    add(
                        RichChatMessage(
                            id = "1",
                            text = "Hello! Glad we connected here. 😊",
                            translation = "नमस्ते! मुझे खुशी है कि हम यहाँ जुड़े। 😊",
                            isMe = false,
                            time = "10:15 AM"
                        )
                    )
                    add(
                        RichChatMessage(
                            id = "2",
                            text = "Are you free to practice English now?",
                            translation = "क्या आप अभी अंग्रेजी का अभ्यास करने के लिए स्वतंत्र हैं?",
                            isMe = false,
                            time = "10:16 AM"
                        )
                    )
                    add(
                        RichChatMessage(
                            id = "3",
                            text = "Could you tell me more about traveling in Japan?",
                            isMe = true,
                            time = "10:18 AM"
                        )
                    )
                }
                contact.name.contains("Yuki", ignoreCase = true) || contact.id == "c_3" -> {
                    add(RichChatMessage("1", "Hello! Glad we connected here. 😊", translation = "नमस्ते! मुझे खुशी है कि हम यहाँ जुड़े। 😊", isMe = false, time = "9:12 AM"))
                    add(RichChatMessage("2", "Your Japanese is improving so fast!", translation = "आपकी जापानी बहुत तेजी से बेहतर हो रही है!", isMe = false, time = "9:12 AM", reactions = listOf("❤️")))
                    add(RichChatMessage("3", "Thank you! 😊\nI'm trying my best!", isMe = true, time = "9:14 AM"))
                    add(RichChatMessage("4", "Do you want to practice a little now? We can talk about travel in Japan! 🇯🇵", translation = "क्या आप अभी थोड़ा अभ्यास करना चाहते हैं? हम जापान यात्रा के बारे में बात कर सकते हैं!", isMe = false, time = "9:15 AM"))
                    add(RichChatMessage("5", "Yes! That sounds great!", isMe = true, time = "9:16 AM"))
                    add(RichChatMessage("6", "", isMe = false, time = "9:17 AM", type = ChatMsgType.AUDIO, audioDuration = "0:12"))
                    add(
                        RichChatMessage(
                            "7",
                            "",
                            isMe = false,
                            time = "9:18 AM",
                            type = ChatMsgType.IMAGE,
                            imageUrl = "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=800",
                            reactions = listOf("❤️", "❤️")
                        )
                    )
                    add(RichChatMessage("8", "This is from my last trip!\nHave you been to Japan?", translation = "यह मेरी पिछली यात्रा की है! क्या आप कभी जापान गए हैं?", isMe = false, time = "9:18 AM"))
                    add(RichChatMessage("9", "Wow! Amazing 😍\nI haven't been yet, but it's on my list!", isMe = true, time = "9:20 AM"))
                }
                else -> {
                    add(RichChatMessage("1", "Hello! Glad we connected here. 😊", translation = "नमस्ते! जुड़ने पर खुशी हुई।", isMe = false, time = "10:15 AM"))
                    add(RichChatMessage("2", contact.lastMsg, translation = "चलो आज साथ अभ्यास करते हैं!", isMe = false, time = "10:16 AM"))
                    add(RichChatMessage("3", "Hey! Thanks for connecting, let's practice together!", isMe = true, time = "10:18 AM"))
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // ==========================================
            // 1. TOP HEADER (Avatar, Name, Flag, 3-dots)
            // ==========================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        FunkyBackArrowIcon(color = Color.Black, modifier = Modifier.size(22.dp))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box(modifier = Modifier.size(42.dp)) {
                        AsyncImage(
                            model = contact.avatar,
                            contentDescription = contact.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )

                        val isOnline = contact.onlineStatus == "online"
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(if (isOnline) Color(0xFF10B981) else Color(0xFF9CA3AF))
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contact.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircleFlag(
                                countryCode = flagCode,
                                modifier = Modifier
                                    .size(13.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = countryName,
                                fontSize = 12.sp,
                                color = Color(0xFF4B5563),
                                fontWeight = FontWeight.Normal
                            )
                            Text(
                                text = " • ",
                                fontSize = 12.sp,
                                color = Color(0xFF9CA3AF)
                            )
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (contact.onlineStatus == "online") Color(0xFF10B981) else Color(0xFF9CA3AF))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (contact.onlineStatus == "online") "Online" else "Offline",
                                fontSize = 12.sp,
                                color = if (contact.onlineStatus == "online") Color(0xFF10B981) else Color(0xFF9CA3AF),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // 3-dots popup
                    Box {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { showOptionsMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            FunkyVerticalMoreIcon(color = Color.Black, modifier = Modifier.size(18.dp))
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("View Profile", color = Color.Black, fontSize = 14.sp) },
                                onClick = {
                                    showOptionsMenu = false
                                    Toast.makeText(context, "Viewing ${contact.name}'s profile", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Invite to Voice Room", color = Color.Black, fontSize = 14.sp) },
                                onClick = {
                                    showOptionsMenu = false
                                    Toast.makeText(context, "Voice room invite sent to ${contact.name}!", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear Chat History", color = Color.Black, fontSize = 14.sp) },
                                onClick = {
                                    showOptionsMenu = false
                                    chatMessages.clear()
                                    Toast.makeText(context, "Chat cleared", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 2. LANGUAGE PARTNER NOTICE BANNER
            // ==========================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFFFFBEB))
                    .border(BorderStroke(1.dp, Color(0xFFFEF3C7)), RoundedCornerShape(18.dp))
                    .clickable {
                        Toast.makeText(context, "You are language partners! Learn together.", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 14.dp, vertical = 11.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        FunkyLightbulbIcon(color = Color(0xFFD97706), modifier = Modifier.size(19.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "You're language partners!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = Color(0xFF1F2937)
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "Help each other learn and make real friends.",
                            fontSize = 11.5.sp,
                            color = Color(0xFF6B7280)
                        )
                    }

                    Text(
                        text = "›",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }

            // ==========================================
            // 3. MESSAGES STREAM
            // ==========================================
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(Color(0xFFE5E7EB).copy(alpha = 0.8f))
                                .padding(horizontal = 16.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "Today",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF4B5563)
                            )
                        }
                    }
                }

                items(chatMessages, key = { it.id }) { msg ->
                    val isSelected = selectedMessageForMenu?.id == msg.id
                    MessageRowItem(
                        msg = msg,
                        contactAvatar = contact.avatar,
                        isVoicePlaying = isVoicePlaying,
                        isHighlighted = isSelected,
                        onLongClick = {
                            selectedMessageForMenu = msg
                            showMoreSubmenu = false
                        },
                        onToggleVoicePlay = {
                            isVoicePlaying = !isVoicePlaying
                            Toast.makeText(
                                context,
                                if (isVoicePlaying) "Playing voice note..." else "Voice paused",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onReactionClick = {
                            val idx = chatMessages.indexOf(msg)
                            if (idx != -1) {
                                val current = chatMessages[idx]
                                val newReactions = if (current.reactions.contains("❤️")) {
                                    current.reactions.filter { it != "❤️" }
                                } else {
                                    current.reactions + "❤️"
                                }
                                chatMessages[idx] = current.copy(reactions = newReactions)
                            }
                        }
                    )
                }
            }

            // ==========================================
            // 4. QUICK TOOLS BAR (Translate, Improve, Example)
            // ==========================================
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .clickable {
                                val updated = chatMessages.map { m ->
                                    if (!m.isMe && m.translation != null) m.copy(isTranslated = !m.isTranslated) else m
                                }
                                chatMessages.clear()
                                chatMessages.addAll(updated)
                                Toast.makeText(context, "Translation toggled for conversations", Toast.LENGTH_SHORT).show()
                            },
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FunkyTranslateIcon(color = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Translate", fontSize = 13.sp, color = Color(0xFF1F2937), fontWeight = FontWeight.Medium)
                        }
                    }
                }

                item {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .clickable {
                                if (messageInputText.isNotBlank()) {
                                    messageInputText = "Could you tell me more about traveling in Japan?"
                                    Toast.makeText(context, "Text phrased naturally!", Toast.LENGTH_SHORT).show()
                                } else {
                                    messageInputText = "I'm really excited to practice with you today!"
                                    Toast.makeText(context, "Suggested message added!", Toast.LENGTH_SHORT).show()
                                }
                            },
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FunkyLightbulbIcon(color = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Improve my text", fontSize = 13.sp, color = Color(0xFF1F2937), fontWeight = FontWeight.Medium)
                        }
                    }
                }

                item {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .clickable {
                                showQuickReplies = !showQuickReplies
                            },
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FunkyBookIcon(color = Color(0xFF4B5563), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Example reply", fontSize = 13.sp, color = Color(0xFF1F2937), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Quick reply chips
            AnimatedVisibility(visible = showQuickReplies) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Thanks so much!", "How was your day?", "Let's practice speaking!").forEach { phrase ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFFFE380))
                                .clickable {
                                    messageInputText = phrase
                                    showQuickReplies = false
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = phrase, fontSize = 11.5.sp, color = Color.Black, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // ==========================================
            // 5. REPLY PREVIEW BANNER
            // ==========================================
            AnimatedVisibility(visible = replyingToMessage != null) {
                replyingToMessage?.let { target ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF3C7))
                            .border(BorderStroke(1.dp, Color(0xFFFDE68A)))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Replying to ${if (target.isMe) "Yourself" else contact.name}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = target.text.ifEmpty { if (target.type == ChatMsgType.IMAGE) "[Photo]" else "[Voice Note]" },
                                fontSize = 12.sp,
                                color = Color(0xFF78350F),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .clickable { replyingToMessage = null },
                            contentAlignment = Alignment.Center
                        ) {
                            FunkyCloseIcon(
                                color = Color(0xFF78350F),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // ==========================================
            // 6. BOTTOM INPUT BAR (+, Input, Emoji, Mic, Send)
            // ==========================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F4F6))
                            .clickable {
                                chatMessages.add(
                                    RichChatMessage(
                                        id = "img_${System.currentTimeMillis()}",
                                        isMe = true,
                                        time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()),
                                        type = ChatMsgType.IMAGE,
                                        imageUrl = "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=800"
                                    )
                                )
                                coroutineScope.launch {
                                    listState.animateScrollToItem(chatMessages.size - 1)
                                }
                                Toast.makeText(context, "Photo attached", Toast.LENGTH_SHORT).show()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "+", fontSize = 24.sp, fontWeight = FontWeight.Normal, color = Color.Black)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(Color(0xFFF9FAFB))
                            .border(BorderStroke(1.dp, Color(0xFFE5E7EB)), RoundedCornerShape(50.dp))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                if (messageInputText.isEmpty()) {
                                    Text(
                                        text = if (editingMessageId != null) "Edit message..." else "Type a message...",
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 14.5.sp
                                    )
                                }
                                BasicTextField(
                                    value = messageInputText,
                                    onValueChange = { messageInputText = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = Color.Black,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    cursorBrush = SolidColor(Color.Black),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            if (messageInputText.isNotBlank()) {
                                                if (editingMessageId != null) {
                                                    val idx = chatMessages.indexOfFirst { it.id == editingMessageId }
                                                    if (idx != -1) {
                                                        chatMessages[idx] = chatMessages[idx].copy(text = messageInputText)
                                                    }
                                                    editingMessageId = null
                                                    messageInputText = ""
                                                    Toast.makeText(context, "Message updated", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    val nowTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                                                    chatMessages.add(
                                                        RichChatMessage(
                                                            id = "msg_${System.currentTimeMillis()}",
                                                            text = messageInputText,
                                                            isMe = true,
                                                            time = nowTime,
                                                            replyToText = replyingToMessage?.text,
                                                            replyToSender = replyingToMessage?.let { if (it.isMe) "You" else contact.name }
                                                        )
                                                    )
                                                    onSendMessage(messageInputText)
                                                    messageInputText = ""
                                                    replyingToMessage = null
                                                    coroutineScope.launch {
                                                        listState.animateScrollToItem(chatMessages.size - 1)
                                                    }
                                                }
                                            }
                                        }
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        messageInputText += " 😊"
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                FunkySmileyIcon(color = Color(0xFF4B5563), modifier = Modifier.size(20.dp))
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        val nowTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                                        chatMessages.add(
                                            RichChatMessage(
                                                id = "audio_${System.currentTimeMillis()}",
                                                isMe = true,
                                                time = nowTime,
                                                type = ChatMsgType.AUDIO,
                                                audioDuration = "0:08"
                                            )
                                        )
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(chatMessages.size - 1)
                                        }
                                        Toast.makeText(context, "Voice note sent!", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                FunkyMicIcon(color = Color(0xFF4B5563), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFC529))
                            .clickable {
                                if (messageInputText.isNotBlank()) {
                                    if (editingMessageId != null) {
                                        val idx = chatMessages.indexOfFirst { it.id == editingMessageId }
                                        if (idx != -1) {
                                            chatMessages[idx] = chatMessages[idx].copy(text = messageInputText)
                                        }
                                        editingMessageId = null
                                        messageInputText = ""
                                        Toast.makeText(context, "Message edited", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val nowTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                                        chatMessages.add(
                                            RichChatMessage(
                                                id = "msg_${System.currentTimeMillis()}",
                                                text = messageInputText,
                                                isMe = true,
                                                time = nowTime,
                                                replyToText = replyingToMessage?.text,
                                                replyToSender = replyingToMessage?.let { if (it.isMe) "You" else contact.name }
                                            )
                                        )
                                        onSendMessage(messageInputText)
                                        messageInputText = ""
                                        replyingToMessage = null
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(chatMessages.size - 1)
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        FunkySendIcon(color = Color.Black, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // =========================================================================
        // 7. CONTEXTUAL LONG-PRESS OVERLAY (Exact UI from Reference Screenshot)
        // =========================================================================
        if (selectedMessageForMenu != null) {
            val targetMsg = selectedMessageForMenu!!

            // Dimmed scrim background (25%)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        selectedMessageForMenu = null
                        showMoreSubmenu = false
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = if (targetMsg.isMe) Alignment.End else Alignment.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    // Highlighted Bubble Card
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (targetMsg.isMe) Color(0xFFFFD54F) else Color.White,
                        border = BorderStroke(2.dp, if (targetMsg.isMe) Color(0xFFFFC529) else Color(0xFFFFC529)),
                        shadowElevation = 12.dp,
                        modifier = Modifier
                            .scale(1.02f)
                            .widthIn(max = 290.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
                            if (targetMsg.type == ChatMsgType.IMAGE) {
                                AsyncImage(
                                    model = targetMsg.imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                            } else {
                                Text(
                                    text = if (targetMsg.isTranslated && targetMsg.translation != null) targetMsg.translation else targetMsg.text.ifEmpty { "Audio message (${targetMsg.audioDuration})" },
                                    fontSize = 15.sp,
                                    color = Color.Black,
                                    lineHeight = 21.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Floating Quick Reactions Pill: ❤️ 😂 👍 😮 😢 ➕
                    Surface(
                        shape = RoundedCornerShape(30.dp),
                        color = Color.White,
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("❤️", "😂", "👍", "😮", "😢").forEach { emoji ->
                                FluentEmojiIcon(
                                    emoji = emoji,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            val idx = chatMessages.indexOfFirst { it.id == targetMsg.id }
                                            if (idx != -1) {
                                                chatMessages[idx] = chatMessages[idx].copy(
                                                    reactions = chatMessages[idx].reactions + emoji
                                                )
                                            }
                                            selectedMessageForMenu = null
                                            Toast.makeText(context, "Reaction added", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(2.dp)
                                )
                            }

                            // Plus Reaction Button
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF3F4F6))
                                    .clickable {
                                        val idx = chatMessages.indexOfFirst { it.id == targetMsg.id }
                                        if (idx != -1) {
                                            chatMessages[idx] = chatMessages[idx].copy(
                                                reactions = chatMessages[idx].reactions + "🔥"
                                            )
                                        }
                                        selectedMessageForMenu = null
                                        Toast.makeText(context, "Reaction added", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "+", fontSize = 17.sp, color = Color(0xFF4B5563))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Context Actions Card
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        shadowElevation = 10.dp,
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        modifier = Modifier.width(225.dp)
                    ) {
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            if (!targetMsg.isMe) {
                                // 1. Translate (Highlighted pill with soft cream yellow background)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFFEF3C7))
                                        .clickable {
                                            val idx = chatMessages.indexOfFirst { it.id == targetMsg.id }
                                            if (idx != -1) {
                                                val curr = chatMessages[idx]
                                                chatMessages[idx] = curr.copy(isTranslated = !curr.isTranslated)
                                            }
                                            selectedMessageForMenu = null
                                            Toast.makeText(context, "Translation toggled!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FunkyTranslateIcon(color = Color.Black, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(text = "Translate", fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                }

                                // 2. Reply
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            replyingToMessage = targetMsg
                                            selectedMessageForMenu = null
                                        }
                                        .padding(horizontal = 18.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FunkyReplyIcon(color = Color.Black, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(text = "Reply", fontSize = 14.5.sp, color = Color(0xFF1F2937), fontWeight = FontWeight.Medium)
                                }

                                // 3. Copy
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(targetMsg.text))
                                            selectedMessageForMenu = null
                                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 18.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FunkyCopyIcon(color = Color.Black, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(text = "Copy", fontSize = 14.5.sp, color = Color(0xFF1F2937), fontWeight = FontWeight.Medium)
                                }

                                // 4. More
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showMoreSubmenu = !showMoreSubmenu }
                                        .padding(horizontal = 18.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FunkyMoreIcon(color = Color.Black, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(text = "More", fontSize = 14.5.sp, color = Color(0xFF1F2937), fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                    Text(text = "›", fontSize = 18.sp, color = Color(0xFF9CA3AF), fontWeight = FontWeight.Bold)
                                }

                                if (showMoreSubmenu) {
                                    HorizontalDivider(color = Color(0xFFF3F4F6))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedMessageForMenu = null
                                                Toast.makeText(context, "Forwarding message...", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 24.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FunkyForwardIcon(color = Color(0xFF4B5563), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(text = "Forward", fontSize = 13.5.sp, color = Color(0xFF4B5563))
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedMessageForMenu = null
                                                Toast.makeText(context, "Report submitted", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 24.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FunkyReportIcon(color = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(text = "Report", fontSize = 13.5.sp, color = Color(0xFFEF4444))
                                    }
                                }
                            } else {
                                // 1. Reply
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            replyingToMessage = targetMsg
                                            selectedMessageForMenu = null
                                        }
                                        .padding(horizontal = 18.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FunkyReplyIcon(color = Color.Black, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(text = "Reply", fontSize = 14.5.sp, color = Color(0xFF1F2937), fontWeight = FontWeight.Medium)
                                }

                                // 2. Copy
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            clipboardManager.setText(AnnotatedString(targetMsg.text))
                                            selectedMessageForMenu = null
                                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 18.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FunkyCopyIcon(color = Color.Black, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(text = "Copy", fontSize = 14.5.sp, color = Color(0xFF1F2937), fontWeight = FontWeight.Medium)
                                }

                                // 3. Edit (if text)
                                if (targetMsg.type == ChatMsgType.TEXT) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                editingMessageId = targetMsg.id
                                                messageInputText = targetMsg.text
                                                selectedMessageForMenu = null
                                            }
                                            .padding(horizontal = 18.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FunkyEditIcon(color = Color.Black, modifier = Modifier.size(17.dp))
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Text(text = "Edit", fontSize = 14.5.sp, color = Color(0xFF1F2937), fontWeight = FontWeight.Medium)
                                    }
                                }

                                // 4. Forward
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedMessageForMenu = null
                                            Toast.makeText(context, "Forwarding message...", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 18.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FunkyForwardIcon(color = Color.Black, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(text = "Forward", fontSize = 14.5.sp, color = Color(0xFF1F2937), fontWeight = FontWeight.Medium)
                                }

                                HorizontalDivider(color = Color(0xFFF3F4F6))

                                // 5. Delete
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            chatMessages.removeIf { it.id == targetMsg.id }
                                            selectedMessageForMenu = null
                                            Toast.makeText(context, "Message deleted", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 18.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FunkyDeleteIcon(color = Color(0xFFEF4444), modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(text = "Delete", fontSize = 14.5.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageRowItem(
    msg: RichChatMessage,
    contactAvatar: String,
    isVoicePlaying: Boolean,
    isHighlighted: Boolean,
    onLongClick: () -> Unit,
    onToggleVoicePlay: () -> Unit,
    onReactionClick: () -> Unit
) {
    val scaleAnim by animateFloatAsState(
        targetValue = if (isHighlighted) 1.02f else 1f,
        animationSpec = spring(),
        label = "scale"
    )

    if (msg.isMe) {
        // ========================
        // SENT MESSAGE (Right Side)
        // ========================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(horizontalAlignment = Alignment.End) {
                // Replied Banner Preview if any
                if (msg.replyToText != null) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFDE68A))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Replying: ${msg.replyToText}",
                            fontSize = 11.sp,
                            color = Color(0xFF78350F),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                when (msg.type) {
                    ChatMsgType.TEXT -> {
                        Surface(
                            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
                            color = Color(0xFFFFD54F),
                            border = if (isHighlighted) BorderStroke(2.dp, Color.White) else null,
                            shadowElevation = if (isHighlighted) 8.dp else 0.dp,
                            modifier = Modifier
                                .scale(scaleAnim)
                                .widthIn(max = 280.dp)
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = onLongClick
                                )
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(
                                    text = msg.text,
                                    fontSize = 14.5.sp,
                                    color = Color.Black,
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Normal
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Row(
                                    modifier = Modifier.align(Alignment.End),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = msg.time,
                                        fontSize = 11.sp,
                                        color = Color(0xFF78350F).copy(alpha = 0.75f),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    FunkyDoubleCheckIcon(
                                        color = Color(0xFF78350F),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        if (msg.reactions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .border(BorderStroke(1.dp, Color(0xFFE5E7EB)), RoundedCornerShape(12.dp))
                                    .clickable { onReactionClick() }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    FluentEmojiIcon(
                                        emoji = msg.reactions.first(),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "${msg.reactions.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }
                    }
                    ChatMsgType.AUDIO -> {
                        Surface(
                            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
                            color = Color(0xFFFFD54F),
                            border = if (isHighlighted) BorderStroke(2.dp, Color.White) else null,
                            shadowElevation = if (isHighlighted) 8.dp else 0.dp,
                            modifier = Modifier
                                .scale(scaleAnim)
                                .widthIn(min = 200.dp, max = 260.dp)
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = onLongClick
                                )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black)
                                        .clickable { onToggleVoicePlay() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isVoicePlaying) {
                                        FunkyPauseIcon(color = Color.White, modifier = Modifier.size(12.dp))
                                    } else {
                                        FunkyPlayIcon(color = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "|||l||l||||l|l||", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.Black)
                                    Text(text = msg.audioDuration, fontSize = 11.sp, color = Color(0xFF78350F))
                                }
                                FunkyDoubleCheckIcon(color = Color(0xFF78350F), modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                    ChatMsgType.IMAGE -> {
                        Surface(
                            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp),
                            color = Color(0xFFFFD54F),
                            border = if (isHighlighted) BorderStroke(2.dp, Color.White) else null,
                            shadowElevation = if (isHighlighted) 8.dp else 0.dp,
                            modifier = Modifier
                                .scale(scaleAnim)
                                .widthIn(max = 260.dp)
                                .combinedClickable(
                                    onClick = {},
                                    onLongClick = onLongClick
                                )
                        ) {
                            Column {
                                AsyncImage(
                                    model = msg.imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = msg.time, fontSize = 11.sp, color = Color(0xFF78350F))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    FunkyDoubleCheckIcon(color = Color(0xFF78350F), modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // ========================
        // RECEIVED MESSAGE (Left Side)
        // ========================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Bottom
        ) {
            AsyncImage(
                model = contactAvatar,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                when (msg.type) {
                    ChatMsgType.TEXT -> {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Column {
                                Surface(
                                    shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
                                    color = Color(0xFFF3F4F6),
                                    border = if (isHighlighted) BorderStroke(2.dp, Color(0xFFFFC529)) else null,
                                    shadowElevation = if (isHighlighted) 8.dp else 0.dp,
                                    modifier = Modifier
                                        .scale(scaleAnim)
                                        .widthIn(max = 260.dp)
                                        .combinedClickable(
                                            onClick = {},
                                            onLongClick = onLongClick
                                        )
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                        Text(
                                            text = if (msg.isTranslated && msg.translation != null) msg.translation else msg.text,
                                            fontSize = 14.5.sp,
                                            color = Color.Black,
                                            lineHeight = 20.sp
                                        )
                                        if (msg.isTranslated) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                FunkyGlobeIcon(color = Color(0xFF2563EB), modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Translated",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF2563EB),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }

                                // Reactions below bubble (if any)
                                if (msg.reactions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White)
                                            .border(BorderStroke(1.dp, Color(0xFFE5E7EB)), RoundedCornerShape(12.dp))
                                            .clickable { onReactionClick() }
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            FluentEmojiIcon(
                                                emoji = msg.reactions.first(),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "${msg.reactions.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = msg.time,
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }

                    ChatMsgType.AUDIO -> {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Surface(
                                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
                                color = Color(0xFFF3F4F6),
                                border = if (isHighlighted) BorderStroke(2.dp, Color(0xFFFFC529)) else null,
                                shadowElevation = if (isHighlighted) 8.dp else 0.dp,
                                modifier = Modifier
                                    .scale(scaleAnim)
                                    .widthIn(min = 200.dp, max = 260.dp)
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = onLongClick
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFFC529))
                                            .clickable { onToggleVoicePlay() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isVoicePlaying) {
                                            FunkyPauseIcon(color = Color.Black, modifier = Modifier.size(13.dp))
                                        } else {
                                            FunkyPlayIcon(color = Color.Black, modifier = Modifier.size(13.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Text(
                                        text = "•||•||||•|||•|",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF4B5563),
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Text(
                                        text = msg.audioDuration,
                                        fontSize = 11.sp,
                                        color = Color(0xFF6B7280),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = msg.time,
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                        }
                    }

                    ChatMsgType.IMAGE -> {
                        Column {
                            Box(modifier = Modifier.widthIn(max = 260.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color(0xFFF3F4F6),
                                    border = if (isHighlighted) BorderStroke(2.dp, Color(0xFFFFC529)) else null,
                                    shadowElevation = if (isHighlighted) 8.dp else 0.dp,
                                    modifier = Modifier
                                        .scale(scaleAnim)
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {},
                                            onLongClick = onLongClick
                                        )
                                ) {
                                    AsyncImage(
                                        model = msg.imageUrl,
                                        contentDescription = "Shared photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = 12.dp, y = (-28).dp)
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(BorderStroke(1.dp, Color(0xFFE5E7EB)), CircleShape)
                                        .clickable { /* Download */ },
                                    contentAlignment = Alignment.Center
                                ) {
                                    FunkyDownloadIcon(color = Color.Black, modifier = Modifier.size(18.dp))
                                }

                                if (msg.reactions.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .offset(x = 12.dp, y = 14.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.White)
                                            .border(BorderStroke(1.dp, Color(0xFFE5E7EB)), RoundedCornerShape(12.dp))
                                            .clickable { onReactionClick() }
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            FluentEmojiIcon(
                                                emoji = msg.reactions.first(),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "${msg.reactions.size}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = msg.time,
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }
                }
            }
        }
    }
}
