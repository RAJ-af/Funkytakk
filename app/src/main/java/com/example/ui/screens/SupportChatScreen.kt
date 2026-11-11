package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.SupabaseSupportMessage
import com.example.SupportChatManager
import com.example.ui.components.SoftBackIcon
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportChatScreen(
    reportId: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val session by SupportChatManager.session.collectAsState()
    val messages by SupportChatManager.messages.collectAsState()
    val isLoading by SupportChatManager.isLoading.collectAsState()
    val isSending by SupportChatManager.isSending.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var remainingTimeText by remember { mutableStateOf("") }

    val isOpen = session?.open == true
    val isAppeal = session?.reason == "appeal" || (!isOpen && session?.can_appeal == true)

    // Live countdown update loop for support_opened session
    LaunchedEffect(session?.until, session?.open, session?.reason) {
        while (isActive) {
            val sess = session
            if (sess == null || !sess.open || SupportChatManager.isSessionExpired(sess)) {
                remainingTimeText = "Chat ended"
            } else if (sess.reason == "appeal") {
                remainingTimeText = "Appeal open"
            } else {
                val formattedUntil = formatUntilTimeString(sess.until)
                remainingTimeText = if (formattedUntil.isNotBlank()) "Chat open until $formattedUntil" else "Chat open"
            }
            delay(1000L)
        }
    }

    // Lifecycle observer to mark conversation read on open/resume & refresh session
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                SupportChatManager.fetchSupportSession(context) {
                    SupportChatManager.markSupportConversationRead(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        SupportChatManager.init(context)
        SupportChatManager.fetchSupportSession(context) {
            SupportChatManager.markSupportConversationRead(context)
        }
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    BackHandler {
        onBack()
    }

    val headerTitle = remember(isOpen, isAppeal) {
        if (isAppeal) "Appeal" else "Support"
    }

    val headerSubtitle = remember(isOpen, isAppeal, remainingTimeText) {
        if (!isOpen) {
            "⚪ Chat closed"
        } else if (isAppeal) {
            "Support Chat"
        } else {
            "🟢 $remainingTimeText"
        }
    }

    Scaffold(
        containerColor = Color(0xFFFCFBF7),
        topBar = {
            Surface(
                color = Color.White,
                shadowElevation = 1.dp,
                border = BorderStroke(1.dp, Color(0xFFF3F4F6))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("support_chat_back_button")
                    ) {
                        SoftBackIcon(
                            color = Color.Black,
                            size = 20.dp
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Speech bubble icon header avatar (matching design mockup)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(GoldAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = headerTitle,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                        Text(
                            text = headerSubtitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }

                    IconButton(onClick = { /* Menu options */ }) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color(0xFF475569)
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (isOpen) {
                Surface(
                    color = Color.White,
                    shadowElevation = 4.dp,
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Paperclip attachment icon
                        IconButton(onClick = { /* Optional attachment */ }) {
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach proof",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    text = "Type a message...",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 14.sp
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("support_chat_input"),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4,
                            enabled = !isSending,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF9FAFB),
                                unfocusedContainerColor = Color(0xFFF9FAFB),
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (inputText.isNotBlank() && !isSending) {
                                    val textToSend = inputText
                                    inputText = ""
                                    SupportChatManager.sendSupportMessage(
                                        context = context,
                                        messageText = textToSend,
                                        onError = { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            })
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank() && !isSending) {
                                    val textToSend = inputText
                                    inputText = ""
                                    SupportChatManager.sendSupportMessage(
                                        context = context,
                                        messageText = textToSend,
                                        onError = { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            },
                            enabled = inputText.isNotBlank() && !isSending,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (inputText.isNotBlank() && !isSending) GoldAccent else Color(0xFFE5E7EB))
                                .testTag("support_chat_send_button")
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                androidx.compose.material3.Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = if (inputText.isNotBlank()) Color.Black else Color(0xFF9CA3AF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!isOpen) {
                // CHAT ENDED FULL SCREEN STATE
                SupportChatEndedState(modifier = Modifier.fillMaxSize())
            } else if (messages.isEmpty() && !isLoading) {
                // EMPTY CHAT STATE (Appeal / First time)
                AppealEmptyChatState(
                    title = if (isAppeal) "Appeal" else "Support",
                    subtitle = if (isAppeal) "Tell us why this decision should be reviewed. Our team will reply here." else "Welcome! Send a message to get in touch with our team.",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // ACTIVE CHAT MESSAGES THREAD
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top banner for support_opened reason
                    if (session?.reason == "support_opened" && !session?.until.isNullOrBlank()) {
                        item {
                            SupportOpenedNoticeCard(untilIso = session?.until)
                        }
                    }

                    items(messages.distinctBy { it.id }, key = { it.id }) { msg ->
                        SupportMessageBubble(message = msg)
                    }
                }
            }

            if (isLoading && messages.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = GoldAccent
                )
            }
        }
    }
}

/**
 * Notice banner displayed at the top of thread when support opens a chat window
 */
@Composable
private fun SupportOpenedNoticeCard(untilIso: String?) {
    val formattedUntil = remember(untilIso) { formatUntilTimeString(untilIso) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFFFBEB),
        border = BorderStroke(1.dp, Color(0xFFFDE68A))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Our team has opened this chat for you. You can reply until $formattedUntil.",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF92400E),
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Empty Chat State (Appeal Illustration, Title & Subtitle)
 */
@Composable
private fun AppealEmptyChatState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Graphic illustration: Overlapping yellow speech bubbles
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background soft bubble
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .offset(x = 24.dp, y = 14.dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 28.dp, bottomEnd = 6.dp))
                    .background(Color(0xFFFEF08A).copy(alpha = 0.6f))
            )
            // Foreground primary yellow bubble with 3 dots
            Surface(
                modifier = Modifier
                    .size(105.dp)
                    .offset(x = (-12).dp, y = (-10).dp),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 8.dp, bottomEnd = 32.dp),
                color = GoldAccent,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                        if (it < 2) Spacer(modifier = Modifier.width(6.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF0F172A),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

/**
 * Full Screen Chat Ended State (Grey bubble with X)
 */
@Composable
private fun SupportChatEndedState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Grey speech bubble with white X illustration
        Box(
            modifier = Modifier.size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(110.dp),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 8.dp, bottomEnd = 32.dp),
                color = Color(0xFF94A3B8),
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "This support chat has ended.",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF0F172A),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "You can no longer send messages in this conversation.",
            fontSize = 14.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun SupportMessageBubble(message: SupabaseSupportMessage) {
    val isUser = message.sender_type == "user"
    val formattedTime = SupportChatManager.formatMessageTime(message.created_at)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // FunkyTalk Support Avatar for admin
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(GoldAccent),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.ChatBubble,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (!isUser) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                ) {
                    Text(
                        text = "FunkyTalk Support",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4B5563)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp
                ),
                color = if (isUser) GoldAccent else Color.White,
                border = BorderStroke(
                    1.dp,
                    if (isUser) Color(0xFFF59E0B) else Color(0xFFE5E7EB)
                ),
                shadowElevation = 0.5.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = message.body,
                        fontSize = 14.sp,
                        color = Color.Black,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formattedTime,
                            fontSize = 10.sp,
                            color = if (isUser) Color(0xFF451A03) else Color(0xFF9CA3AF)
                        )
                        if (isUser) {
                            Spacer(modifier = Modifier.width(4.dp))
                            androidx.compose.material3.Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Sent",
                                tint = Color(0xFF451A03),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatUntilTimeString(untilIso: String?): String {
    if (untilIso.isNullOrBlank()) return ""
    return try {
        val clean = untilIso.replace("Z", "+0000").replace("T", " ")
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = format.parse(clean) ?: return ""
        val outFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        outFormat.format(date)
    } catch (_: Exception) {
        ""
    }
}
