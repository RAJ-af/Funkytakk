package com.example.ui.screens.chat

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.ui.screens.chat.components.*
import com.example.ui.screens.chat.model.ChatMessage
import com.example.ui.screens.chat.model.ChatMessageType
import com.example.ui.screens.chat.model.ChatSampleData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DirectChatScreen(
    partnerName: String = "Yuki Tanaka",
    partnerAvatarUrl: String = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
    partnerCountry: String = "Japan",
    partnerCountryCode: String = "jp",
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Message list state initialized with sample data
    val messages = remember { mutableStateListOf<ChatMessage>().apply { addAll(ChatSampleData.getInitialMessages()) } }
    var inputText by remember { mutableStateOf("") }

    // Contextual Long-press Menu state
    var selectedMessageForMenu by remember { mutableStateOf<ChatMessage?>(null) }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }

    // Audio Playback state
    var playingAudioId by remember { mutableStateOf<String?>(null) }

    // Active playing message timer
    LaunchedEffect(playingAudioId) {
        if (playingAudioId != null) {
            val targetMsg = messages.find { it.id == playingAudioId }
            val dur = targetMsg?.audioDurationSecs ?: 12
            delay(dur * 1000L)
            playingAudioId = null
        }
    }

    BackHandler {
        if (selectedMessageForMenu != null) {
            selectedMessageForMenu = null
        } else {
            onBack()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFFAFAEE))) {
        Scaffold(
            containerColor = Color(0xFFFAFAEE), // Warm off-white background #fafaee
            topBar = {
                ChatHeader(
                    partnerName = partnerName,
                    partnerAvatarUrl = partnerAvatarUrl,
                    partnerCountry = partnerCountry,
                    partnerCountryCode = partnerCountryCode,
                    isOnline = true,
                    onBackClick = onBack,
                    onMenuClick = {
                        Toast.makeText(context, "Options for $partnerName", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            bottomBar = {
                Column(modifier = Modifier.background(Color(0xFFFAFAEE))) {
                    // AI Quick Action Buttons Row (Translate, Improve my text, Example reply)
                    AIActionsRow(
                        onTranslateClick = {
                            val lastReceived = messages.findLast { !it.isMe && it.type == ChatMessageType.TEXT }
                            if (lastReceived != null) {
                                val idx = messages.indexOf(lastReceived)
                                if (idx != -1) {
                                    val isCurrentlyTrans = lastReceived.isTranslated
                                    messages[idx] = lastReceived.copy(
                                        isTranslated = !isCurrentlyTrans,
                                        translation = if (!isCurrentlyTrans) "Your Japanese is improving so rapidly!" else null
                                    )
                                    Toast.makeText(context, if (!isCurrentlyTrans) "Translated to English" else "Original text shown", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "No received message to translate", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onImproveTextClick = {
                            if (inputText.isNotBlank()) {
                                inputText = "Thank you so much! I'm genuinely trying my best every day."
                                Toast.makeText(context, "Polished your sentence!", Toast.LENGTH_SHORT).show()
                            } else {
                                inputText = "I would love to visit Kyoto and Mount Fuji someday!"
                                Toast.makeText(context, "Practice suggestion added!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onExampleReplyClick = {
                            inputText = "That sounds amazing! What was your favorite place during the trip?"
                            Toast.makeText(context, "Example reply ready!", Toast.LENGTH_SHORT).show()
                        }
                    )

                    // Message Composer
                    MessageComposer(
                        messageText = inputText,
                        onMessageChange = { inputText = it },
                        onSendMessage = {
                            if (inputText.isNotBlank()) {
                                val nowTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                                messages.add(
                                    ChatMessage(
                                        id = "text_${System.currentTimeMillis()}",
                                        text = inputText.trim(),
                                        time = nowTime,
                                        isMe = true,
                                        type = ChatMessageType.TEXT,
                                        replyToText = replyingToMessage?.text,
                                        replyToSender = replyingToMessage?.let { if (it.isMe) "You" else partnerName }
                                    )
                                )
                                inputText = ""
                                replyingToMessage = null
                                coroutineScope.launch {
                                    listState.animateScrollToItem(messages.size - 1)
                                }
                            }
                        },
                        onAddClick = {
                            Toast.makeText(context, "Attachment options", Toast.LENGTH_SHORT).show()
                        },
                        onSmileyClick = {
                            val lastMsg = messages.lastOrNull()
                            if (lastMsg != null) {
                                selectedMessageForMenu = lastMsg
                            }
                        },
                        onMicClick = {
                            val nowTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                            messages.add(
                                ChatMessage(
                                    id = "voice_${System.currentTimeMillis()}",
                                    time = nowTime,
                                    isMe = true,
                                    type = ChatMessageType.AUDIO,
                                    audioDuration = "0:08",
                                    audioDurationSecs = 8
                                )
                            )
                            coroutineScope.launch {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                            Toast.makeText(context, "Voice message sent", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        ) { paddingValues ->
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                // Date separator: "Today"
                item {
                    DateSeparatorPill(dateText = "Today")
                }

                // Dynamic message bubbles
                items(messages, key = { it.id }) { msg ->
                    when (msg.type) {
                        ChatMessageType.TEXT -> {
                            if (msg.isMe) {
                                OutgoingMessageBubble(
                                    message = msg,
                                    isHighlighted = selectedMessageForMenu?.id == msg.id,
                                    onLongClick = { selectedMessageForMenu = msg }
                                )
                            } else {
                                IncomingMessageBubble(
                                    message = msg,
                                    partnerAvatarUrl = partnerAvatarUrl,
                                    isHighlighted = selectedMessageForMenu?.id == msg.id,
                                    onLongClick = { selectedMessageForMenu = msg },
                                    onReactionClick = { selectedMessageForMenu = msg }
                                )
                            }
                        }
                        ChatMessageType.AUDIO -> {
                            VoiceMessageBubble(
                                message = msg,
                                partnerAvatarUrl = partnerAvatarUrl,
                                isPlaying = playingAudioId == msg.id,
                                isHighlighted = selectedMessageForMenu?.id == msg.id,
                                onPlayPauseClick = {
                                    playingAudioId = if (playingAudioId == msg.id) null else msg.id
                                },
                                onLongClick = { selectedMessageForMenu = msg }
                            )
                        }
                        ChatMessageType.IMAGE -> {
                            ImageMessageBubble(
                                message = msg,
                                partnerAvatarUrl = partnerAvatarUrl,
                                isHighlighted = selectedMessageForMenu?.id == msg.id,
                                onDownloadClick = {
                                    Toast.makeText(context, "Downloading photo...", Toast.LENGTH_SHORT).show()
                                },
                                onReactionClick = { selectedMessageForMenu = msg },
                                onLongClick = { selectedMessageForMenu = msg }
                            )
                        }
                    }
                }
            }
        }

        // Contextual Long-press Overlay
        if (selectedMessageForMenu != null) {
            val target = selectedMessageForMenu!!
            MessageContextMenu(
                targetMessage = target,
                onDismiss = { selectedMessageForMenu = null },
                onSelectReaction = { reaction ->
                    val idx = messages.indexOfFirst { it.id == target.id }
                    if (idx != -1) {
                        val currentReactions = target.reactions.toMutableList()
                        if (currentReactions.contains(reaction)) {
                            currentReactions.remove(reaction)
                        } else {
                            currentReactions.add(reaction)
                        }
                        messages[idx] = target.copy(reactions = currentReactions)
                    }
                    selectedMessageForMenu = null
                },
                onTranslate = {
                    val idx = messages.indexOfFirst { it.id == target.id }
                    if (idx != -1) {
                        messages[idx] = target.copy(
                            isTranslated = true,
                            translation = "Your Japanese is improving so rapidly!"
                        )
                        Toast.makeText(context, "Message translated!", Toast.LENGTH_SHORT).show()
                    }
                    selectedMessageForMenu = null
                },
                onReply = {
                    replyingToMessage = target
                    Toast.makeText(context, "Replying to message", Toast.LENGTH_SHORT).show()
                    selectedMessageForMenu = null
                },
                onCopy = {
                    clipboardManager.setText(AnnotatedString(target.text))
                    Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                    selectedMessageForMenu = null
                },
                onReactMore = {
                    val idx = messages.indexOfFirst { it.id == target.id }
                    if (idx != -1) {
                        val currentReactions = target.reactions.toMutableList()
                        currentReactions.add("❤️")
                        messages[idx] = target.copy(reactions = currentReactions)
                    }
                    Toast.makeText(context, "Reaction added", Toast.LENGTH_SHORT).show()
                    selectedMessageForMenu = null
                },
                onMoreActions = {
                    Toast.makeText(context, "More options", Toast.LENGTH_SHORT).show()
                    selectedMessageForMenu = null
                }
            )
        }
    }
}
