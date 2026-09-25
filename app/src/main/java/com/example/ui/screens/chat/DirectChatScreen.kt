package com.example.ui.screens.chat

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.crypto.VaultSecurityManager
import com.example.crypto.VaultStatus
import com.example.repository.EncryptedChatRepository
import com.example.repository.RecipientNotReadyException
import com.example.ui.screens.chat.components.*
import com.example.ui.screens.chat.model.ChatMessage
import com.example.ui.screens.chat.model.ChatMessageType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DirectChatScreen(
    partnerUserId: String = "",
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

    val vaultStatus by VaultSecurityManager.vaultStatus.collectAsState()
    var conversationId by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    // Edge case check state: does recipient have public encryption key?
    var recipientKeyReady by remember { mutableStateOf<Boolean?>(null) }
    var isCheckingKey by remember { mutableStateOf(false) }
    var showNotReadyDialog by remember { mutableStateOf(false) }

    // Initialize repo, resolve conversation, and check recipient key availability
    LaunchedEffect(partnerUserId, vaultStatus) {
        EncryptedChatRepository.init(context)
        if (vaultStatus == VaultStatus.UNLOCKED && partnerUserId.isNotBlank()) {
            try {
                val conv = EncryptedChatRepository.getOrCreateLocalConversation(
                    context = context,
                    otherUserId = partnerUserId,
                    otherUserName = partnerName,
                    otherUserAvatar = partnerAvatarUrl
                )
                conversationId = conv.id
                EncryptedChatRepository.markConversationRead(conv.id)

                // Verify if recipient has registered E2EE public keys
                isCheckingKey = true
                recipientKeyReady = EncryptedChatRepository.checkRecipientKeyStatus(context, partnerUserId)
                isCheckingKey = false
            } catch (e: Exception) {
                android.util.Log.e("DirectChatScreen", "Error resolving conversation", e)
            }
        }
    }

    // Collect messages from encrypted Room database
    val localDbMessages by if (conversationId.isNotBlank()) {
        EncryptedChatRepository.getMessagesFlow(conversationId).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // Map local DB messages to UI ChatMessage
    val uiMessages = remember(localDbMessages) {
        localDbMessages.map { dbMsg ->
            ChatMessage(
                id = dbMsg.id,
                text = dbMsg.text,
                time = timeFormatter.format(Date(dbMsg.timestamp)),
                isMe = dbMsg.isOutgoing,
                type = ChatMessageType.TEXT,
                replyToText = dbMsg.replyToText,
                replyToSender = dbMsg.replyToSender,
                showAvatar = !dbMsg.isOutgoing
            )
        }
    }

    var inputText by remember { mutableStateOf("") }
    var selectedMessageForMenu by remember { mutableStateOf<ChatMessage?>(null) }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(uiMessages.size) {
        if (uiMessages.isNotEmpty()) {
            listState.animateScrollToItem(uiMessages.size - 1)
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
            containerColor = Color(0xFFFAFAEE),
            topBar = {
                ChatHeader(
                    partnerName = partnerName,
                    partnerAvatarUrl = partnerAvatarUrl,
                    partnerCountry = partnerCountry,
                    partnerCountryCode = partnerCountryCode,
                    isOnline = true,
                    onBackClick = onBack,
                    onMenuClick = {
                        Toast.makeText(context, "End-to-End Encrypted Chat with $partnerName", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            bottomBar = {
                Column(modifier = Modifier.background(Color(0xFFFAFAEE))) {
                    // AI Quick Action Buttons Row
                    AIActionsRow(
                        onTranslateClick = {
                            val lastReceived = uiMessages.findLast { !it.isMe && it.type == ChatMessageType.TEXT }
                            if (lastReceived != null) {
                                Toast.makeText(context, "Original text shown", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No received message to translate", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onImproveTextClick = {
                            if (inputText.isNotBlank()) {
                                inputText = "Thank you so much! I'm genuinely trying my best every day."
                                Toast.makeText(context, "Polished your sentence!", Toast.LENGTH_SHORT).show()
                            } else {
                                inputText = "I would love to practice speaking together soon!"
                                Toast.makeText(context, "Practice suggestion added!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onExampleReplyClick = {
                            inputText = "That sounds wonderful! When are you free for a conversation?"
                            Toast.makeText(context, "Example reply ready!", Toast.LENGTH_SHORT).show()
                        }
                    )

                    // Message Composer
                    MessageComposer(
                        messageText = inputText,
                        onMessageChange = { inputText = it },
                        onSendMessage = {
                            if (inputText.isNotBlank() && !isSending) {
                                val textToSend = inputText.trim()
                                val replyText = replyingToMessage?.text
                                val replySender = replyingToMessage?.let { if (it.isMe) "You" else partnerName }
                                isSending = true

                                coroutineScope.launch {
                                    val sendResult = EncryptedChatRepository.sendMessage(
                                        context = context,
                                        recipientUserId = partnerUserId.ifBlank { "partner_mock_id" },
                                        recipientName = partnerName,
                                        recipientAvatar = partnerAvatarUrl,
                                        text = textToSend,
                                        replyToText = replyText,
                                        replyToSender = replySender
                                    )
                                    isSending = false
                                    sendResult.onSuccess {
                                        inputText = ""
                                        replyingToMessage = null
                                        recipientKeyReady = true
                                    }.onFailure { ex ->
                                        if (ex is RecipientNotReadyException) {
                                            recipientKeyReady = false
                                            showNotReadyDialog = true
                                            // Do NOT clear inputText so user doesn't lose what they typed!
                                        } else {
                                            Toast.makeText(context, ex.message ?: "Failed to encrypt and send message", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        },
                        onAddClick = {
                            Toast.makeText(context, "Encrypted attachment options", Toast.LENGTH_SHORT).show()
                        },
                        onSmileyClick = {
                            val lastMsg = uiMessages.lastOrNull()
                            if (lastMsg != null) {
                                selectedMessageForMenu = lastMsg
                            }
                        },
                        onMicClick = {
                            Toast.makeText(context, "Encrypted voice messaging", Toast.LENGTH_SHORT).show()
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
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
            ) {
                // Date separator
                item {
                    DateSeparatorPill(dateText = "Today")
                }

                if (uiMessages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (recipientKeyReady == false) {
                                    "$partnerName hasn't enabled secure chat yet.\nYou can message them once they open FunkyTalk."
                                } else {
                                    "No messages yet.\nSend a message to start chatting with $partnerName!"
                                },
                                fontSize = 14.sp,
                                color = Color(0xFF9CA3AF),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                // Dynamic message bubbles
                items(uiMessages, key = { it.id }) { msg ->
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
            }
        }

        // Contextual Long-press Overlay
        if (selectedMessageForMenu != null) {
            val target = selectedMessageForMenu!!
            MessageContextMenu(
                targetMessage = target,
                onDismiss = { selectedMessageForMenu = null },
                onSelectReaction = { reaction ->
                    selectedMessageForMenu = null
                },
                onTranslate = {
                    Toast.makeText(context, "Translation shown", Toast.LENGTH_SHORT).show()
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
                    selectedMessageForMenu = null
                },
                onMoreActions = {
                    selectedMessageForMenu = null
                }
            )
        }

        // Prompt vault setup or unlock if not unlocked
        if (vaultStatus != VaultStatus.UNLOCKED) {
            VaultManagementDialogs(onDismissRequest = onBack)
        }

        // Graceful Edge Case Dialog when recipient hasn't opened app / initialized keys
        if (showNotReadyDialog) {
            Dialog(
                onDismissRequest = { showNotReadyDialog = false },
                properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    shadowElevation = 16.dp,
                    border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Icon in amber/gold soft container
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(Color(0xFFFEF3C7), RoundedCornerShape(30.dp))
                                .border(1.5.dp, Color(0xFFFDE68A), RoundedCornerShape(30.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Security Notice",
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Secure Chat Unavailable",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Color(0xFF111827),
                            textAlign = TextAlign.Center,
                            letterSpacing = (-0.3).sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "This user is not ready for secure chat yet.",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFFB45309),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "$partnerName has not opened FunkyTalk yet, so their end-to-end encryption keys have not been registered.\n\nAs soon as $partnerName launches the app, secure chat will be ready automatically.\n\nYour message draft has been saved.",
                            fontSize = 13.sp,
                            color = Color(0xFF4B5563),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { showNotReadyDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Got it",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                showNotReadyDialog = false
                                coroutineScope.launch {
                                    isCheckingKey = true
                                    val ready = EncryptedChatRepository.checkRecipientKeyStatus(context, partnerUserId)
                                    recipientKeyReady = ready
                                    isCheckingKey = false
                                    if (ready == true) {
                                        Toast.makeText(context, "$partnerName is now ready for secure chat!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "$partnerName is still not ready.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Check Again",
                                tint = Color(0xFF4B5563),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Check Again",
                                color = Color(0xFF4B5563),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
