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
import com.example.ui.components.*
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
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Message list state initialized with the exact sample data from screenshot
    val messages = remember { mutableStateListOf<ChatMessage>().apply { addAll(ChatSampleData.getInitialMessages()) } }
    var inputText by remember { mutableStateOf("") }

    // Contextual Long-press Menu state
    var selectedMessageForMenu by remember { mutableStateOf<ChatMessage?>(null) }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }

    // Audio Playback state
    var playingAudioId by remember { mutableStateOf<String?>(null) }
    var audioProgress by remember { mutableStateOf(0f) }

    // Voice recording & preview states
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingDurationSeconds by remember { mutableStateOf(0) }
    var isVoicePreviewActive by remember { mutableStateOf(false) }
    var previewDurationSeconds by remember { mutableStateOf(12) }
    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewPlaybackProgress by remember { mutableStateOf(0f) }

    // Recording timer coroutine
    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            recordingDurationSeconds = 0
            while (isRecordingVoice) {
                delay(1000)
                recordingDurationSeconds++
            }
        }
    }

    // Preview audio player timer
    LaunchedEffect(isPreviewPlaying) {
        if (isPreviewPlaying) {
            previewPlaybackProgress = 0f
            val steps = 60
            val delayPerStep = (previewDurationSeconds * 1000L) / steps
            for (i in 1..steps) {
                if (!isPreviewPlaying) break
                delay(delayPerStep)
                previewPlaybackProgress = i.toFloat() / steps.toFloat()
            }
            isPreviewPlaying = false
            previewPlaybackProgress = 0f
        }
    }

    // Active playing message timer
    LaunchedEffect(playingAudioId) {
        if (playingAudioId != null) {
            audioProgress = 0f
            val steps = 60
            val targetMsg = messages.find { it.id == playingAudioId }
            val dur = targetMsg?.audioDurationSecs ?: 12
            val delayPerStep = (dur * 1000L) / steps
            for (i in 1..steps) {
                if (playingAudioId == null) break
                delay(delayPerStep)
                audioProgress = i.toFloat() / steps.toFloat()
            }
            playingAudioId = null
            audioProgress = 0f
        }
    }

    BackHandler {
        if (selectedMessageForMenu != null) {
            selectedMessageForMenu = null
        } else {
            onBack()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.White)) {
        Scaffold(
            containerColor = Color.White,
            topBar = {
                ChatTopBar(
                    partnerName = partnerName,
                    partnerAvatarUrl = partnerAvatarUrl,
                    partnerCountry = partnerCountry,
                    isOnline = true,
                    onBackClick = onBack,
                    onMenuClick = {
                        Toast.makeText(context, "Chat options for $partnerName", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            bottomBar = {
                Column(modifier = Modifier.background(Color.White)) {
                    // Quick Action Pills Row
                    ChatActionPillsBar(
                        onTranslateClick = {
                            val lastReceived = messages.findLast { !it.isMe && it.type == ChatMessageType.TEXT }
                            if (lastReceived != null) {
                                val idx = messages.indexOf(lastReceived)
                                if (idx != -1) {
                                    val isCurrentlyTrans = lastReceived.isTranslated
                                    messages[idx] = lastReceived.copy(
                                        isTranslated = !isCurrentlyTrans,
                                        translation = if (!isCurrentlyTrans) "Aapki Japanese bohot tezi se improve ho rahi hai!" else null
                                    )
                                    Toast.makeText(context, if (!isCurrentlyTrans) "Translated to English/Hindi" else "Original text shown", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "No received text message to translate", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onImproveTextClick = {
                            if (inputText.isNotBlank()) {
                                inputText = "Thank you so much! I'm genuinely trying my very best every day 😊"
                                Toast.makeText(context, "Polished your sentence!", Toast.LENGTH_SHORT).show()
                            } else {
                                inputText = "I would love to visit Kyoto and Mount Fuji someday! 🗾🌸"
                                Toast.makeText(context, "Suggested practice sentence inserted!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onExampleReplyClick = {
                            inputText = "That sounds lovely! What was your favorite place during the trip? ⛩️"
                            Toast.makeText(context, "Example reply ready to send!", Toast.LENGTH_SHORT).show()
                        }
                    )

                    // Bottom Bar based on current state (Live Recording, Preview, or Standard Input)
                    when {
                        isRecordingVoice -> {
                            VoiceRecordingLiveBar(
                                recordingDurationSeconds = recordingDurationSeconds,
                                onStopRecording = {
                                    previewDurationSeconds = recordingDurationSeconds.coerceAtLeast(3)
                                    isRecordingVoice = false
                                    isVoicePreviewActive = true
                                },
                                onCancelRecording = {
                                    isRecordingVoice = false
                                    recordingDurationSeconds = 0
                                    Toast.makeText(context, "Voice note cancelled", Toast.LENGTH_SHORT).show()
                                },
                                onLockRecording = {
                                    Toast.makeText(context, "Recording locked 🔒", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        isVoicePreviewActive -> {
                            VoicePreviewBar(
                                durationString = String.format(Locale.getDefault(), "0:%02d", previewDurationSeconds),
                                isPlaying = isPreviewPlaying,
                                playbackProgress = previewPlaybackProgress,
                                onTogglePlay = {
                                    isPreviewPlaying = !isPreviewPlaying
                                },
                                onDelete = {
                                    isVoicePreviewActive = false
                                    isPreviewPlaying = false
                                    previewPlaybackProgress = 0f
                                    Toast.makeText(context, "Voice note deleted", Toast.LENGTH_SHORT).show()
                                },
                                onSend = {
                                    val nowTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
                                    val durStr = String.format(Locale.getDefault(), "0:%02d", previewDurationSeconds)
                                    messages.add(
                                        ChatMessage(
                                            id = "audio_${System.currentTimeMillis()}",
                                            isMe = true,
                                            time = nowTime,
                                            type = ChatMessageType.AUDIO,
                                            audioDuration = durStr,
                                            audioDurationSecs = previewDurationSeconds,
                                            replyToText = replyingToMessage?.text,
                                            replyToSender = replyingToMessage?.let { if (it.isMe) "You" else partnerName }
                                        )
                                    )
                                    isVoicePreviewActive = false
                                    isPreviewPlaying = false
                                    previewPlaybackProgress = 0f
                                    replyingToMessage = null
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(messages.size - 1)
                                    }
                                    Toast.makeText(context, "Voice note sent!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        else -> {
                            ChatBottomInputBar(
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
                                onPlusClick = {
                                    Toast.makeText(context, "Share photos, documents or location", Toast.LENGTH_SHORT).show()
                                },
                                onSmileyClick = {
                                    inputText += " 😊"
                                },
                                onMicClick = {
                                    isRecordingVoice = true
                                }
                            )
                        }
                    }
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
                // Top Banner: "You're language partners! 🎉"
                item {
                    LanguagePartnerBanner(
                        onClick = {
                            Toast.makeText(context, "Language partner exchange info & goals", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Date separator: "Today"
                item {
                    DateSeparatorPill(dateText = "Today")
                }

                // Messages list
                items(messages, key = { it.id }) { msg ->
                    ChatMessageItem(
                        message = msg,
                        partnerAvatarUrl = partnerAvatarUrl,
                        isPlayingAudio = playingAudioId == msg.id,
                        isHighlighted = selectedMessageForMenu?.id == msg.id,
                        onLongClick = {
                            selectedMessageForMenu = msg
                        },
                        onToggleAudioPlay = {
                            if (playingAudioId == msg.id) {
                                playingAudioId = null
                            } else {
                                playingAudioId = msg.id
                            }
                        },
                        onReactionClick = {
                            selectedMessageForMenu = msg
                        },
                        onDownloadImageClick = {
                            Toast.makeText(context, "Downloading full resolution image...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        // Contextual Menu Overlay when message is long-pressed (Image 2)
        if (selectedMessageForMenu != null) {
            val target = selectedMessageForMenu!!
            ChatMessageContextMenuOverlay(
                targetMessage = target,
                onDismiss = { selectedMessageForMenu = null },
                onSelectEmoji = { emoji ->
                    val idx = messages.indexOfFirst { it.id == target.id }
                    if (idx != -1) {
                        val currentReactions = target.reactions.toMutableList()
                        if (currentReactions.contains(emoji)) {
                            currentReactions.remove(emoji)
                        } else {
                            currentReactions.add(emoji)
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
                            translation = "Aapki Japanese bohot tezi se improve ho rahi hai!"
                        )
                        Toast.makeText(context, "Message translated!", Toast.LENGTH_SHORT).show()
                    }
                    selectedMessageForMenu = null
                },
                onReply = {
                    replyingToMessage = target
                    Toast.makeText(context, "Replying to ${if (target.isMe) "yourself" else partnerName}", Toast.LENGTH_SHORT).show()
                    selectedMessageForMenu = null
                },
                onCopy = {
                    clipboardManager.setText(AnnotatedString(target.text))
                    Toast.makeText(context, "Message copied to clipboard", Toast.LENGTH_SHORT).show()
                    selectedMessageForMenu = null
                },
                onReactMore = {
                    val idx = messages.indexOfFirst { it.id == target.id }
                    if (idx != -1) {
                        val currentReactions = target.reactions.toMutableList()
                        currentReactions.add("🎉")
                        messages[idx] = target.copy(reactions = currentReactions)
                    }
                    Toast.makeText(context, "Reaction added!", Toast.LENGTH_SHORT).show()
                    selectedMessageForMenu = null
                },
                onMoreActions = {
                    Toast.makeText(context, "Forward / Share / Report options", Toast.LENGTH_SHORT).show()
                    selectedMessageForMenu = null
                }
            )
        }
    }
}
