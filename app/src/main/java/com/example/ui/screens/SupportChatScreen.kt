package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
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
import com.example.ui.components.*
import com.example.ui.screens.chat.components.*
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

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
    var isChatEnded by remember { mutableStateOf(false) }

    // Live countdown update loop every second
    LaunchedEffect(session?.until, session?.open) {
        while (isActive) {
            val sess = session
            if (sess == null || !sess.open || SupportChatManager.isSessionExpired(sess.until)) {
                isChatEnded = true
                remainingTimeText = "Chat ended"
            } else {
                isChatEnded = false
                val formatted = SupportChatManager.formatRemainingCountdown(sess.until)
                remainingTimeText = "Support chat open for $formatted"
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

                    // FunkyTalk Official Brand Avatar
                    Box(modifier = Modifier.size(44.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(GoldAccent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "FT",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color.Black
                            )
                        }

                        // Verified Badge overlay on bottom right
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(Color(0xFF3B82F6))
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            SoftCheckIcon(
                                color = Color.White,
                                size = 9.dp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "FunkyTalk Support",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFFFFBEB))
                                    .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "OFFICIAL",
                                    color = Color(0xFFB45309),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isChatEnded) "This support chat has ended." else remainingTimeText,
                            fontSize = 12.sp,
                            fontWeight = if (!isChatEnded) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isChatEnded) Color(0xFFEF4444) else Color(0xFFB45309)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, Color(0xFFE5E7EB))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    if (isChatEnded) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFEE2E2),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SoftShieldIcon(size = 18.dp, color = Color(0xFFDC2626))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "This support chat has ended.",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB91C1C)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                placeholder = {
                                    Text(
                                        text = "Type a message to FunkyTalk Support...",
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
                                    .size(48.dp)
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
                                    AppSvgIcon(
                                        icon = ChatIconAsset.Send,
                                        tint = if (inputText.isNotBlank()) Color.Black else Color(0xFF9CA3AF),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
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
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Official Welcome Note Card at the top of thread
                item {
                    OfficialWelcomeCard(isEnded = isChatEnded)
                }

                items(messages.distinctBy { it.id }, key = { it.id }) { msg ->
                    SupportMessageBubble(message = msg)
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

@Composable
private fun OfficialWelcomeCard(isEnded: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (isEnded) Color(0xFFF1F5F9) else Color(0xFFFFFBEB),
        border = BorderStroke(1.dp, if (isEnded) Color(0xFFCBD5E1) else Color(0xFFFDE68A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SoftHeadsetIcon(
                    size = 22.dp,
                    color = if (isEnded) Color(0xFF475569) else Color(0xFFB45309)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FunkyTalk Official Support",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isEnded) Color(0xFF1E293B) else Color(0xFF92400E)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isEnded) {
                    "This support session has ended. If moderators need additional information, they will open a new chat window."
                } else {
                    "Welcome to your personal support channel. Have questions about your account or safety guidelines? Reply directly to this message."
                },
                fontSize = 13.sp,
                color = if (isEnded) Color(0xFF475569) else Color(0xFF78350F),
                lineHeight = 18.sp
            )
        }
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
        // FunkyTalk Official avatar on left for admin messages
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(GoldAccent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "FT",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = Color.Black
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            // Header for admin message: ALWAYS "FunkyTalk"
            if (!isUser) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
                ) {
                    Text(
                        text = "FunkyTalk",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4B5563)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    SoftCheckIcon(
                        color = Color(0xFF3B82F6),
                        size = 10.dp
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

                    Text(
                        text = formattedTime,
                        fontSize = 10.sp,
                        color = if (isUser) Color(0xFF451A03) else Color(0xFF9CA3AF),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}
