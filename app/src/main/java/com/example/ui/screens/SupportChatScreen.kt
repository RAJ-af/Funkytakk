package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.example.ui.components.FunkyPlusIcon
import com.example.ui.components.FunkySendIcon
import com.example.ui.components.FunkySingleCheckIcon
import com.example.ui.components.FunkyVerticalMoreIcon
import com.example.ui.components.SoftBackIcon
import com.example.ui.components.SoftChatBubbleIcon
import com.example.ui.components.SoftCloseIcon
import com.example.ui.theme.LocalFunkyColors
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
    val funkyColors = LocalFunkyColors.current
    val isDark = funkyColors.isDark
    val listState = rememberLazyListState()

    val session by SupportChatManager.session.collectAsState()
    val messages by SupportChatManager.messages.collectAsState()
    val isLoading by SupportChatManager.isLoading.collectAsState()
    val isSending by SupportChatManager.isSending.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var remainingTimeText by remember { mutableStateOf("") }

    val isAppeal = session?.reason == "appeal"

    // Live countdown update loop for support_opened session; 24/7 always open otherwise
    LaunchedEffect(session?.until, session?.open, session?.reason) {
        while (isActive) {
            val sess = session
            remainingTimeText = when {
                sess?.reason == "appeal" -> "Appeal open"
                sess?.reason == "support_opened" && !sess.until.isNullOrBlank() -> {
                    val formattedUntil = formatUntilTimeString(sess.until)
                    if (formattedUntil.isNotBlank()) "Chat open until $formattedUntil" else "Online · 24/7 Support"
                }
                else -> "Online · 24/7 Support"
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
        com.example.TelemetryManager.logSupportChatOpened()
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

    val headerTitle = remember(isAppeal) {
        if (isAppeal) "Appeal" else "Support"
    }

    val headerSubtitle = remember(isAppeal, remainingTimeText) {
        if (isAppeal) {
            "Support Chat"
        } else {
            remainingTimeText.ifBlank { "Online · 24/7 Support" }
        }
    }

    Scaffold(
        containerColor = funkyColors.bg,
        topBar = {
            Surface(
                color = funkyColors.surface,
                shadowElevation = if (isDark) 0.dp else 1.dp,
                border = BorderStroke(1.dp, funkyColors.line)
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
                            color = funkyColors.ink,
                            size = 20.dp
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(funkyColors.mango),
                        contentAlignment = Alignment.Center
                    ) {
                        SoftChatBubbleIcon(
                            color = funkyColors.onMango,
                            size = 20.dp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = headerTitle,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = funkyColors.ink
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isAppeal) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(funkyColors.ok)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = headerSubtitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = funkyColors.mute
                            )
                        }
                    }

                    IconButton(onClick = { /* Menu options */ }) {
                        FunkyVerticalMoreIcon(
                            color = funkyColors.mute,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                    color = funkyColors.surface,
                    shadowElevation = if (isDark) 0.dp else 4.dp,
                    border = BorderStroke(1.dp, funkyColors.line)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { /* Optional attachment */ }) {
                            FunkyPlusIcon(
                                color = funkyColors.mute,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    text = "Type a message...",
                                    color = funkyColors.mute,
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
                                focusedContainerColor = funkyColors.bg,
                                unfocusedContainerColor = funkyColors.bg,
                                focusedBorderColor = funkyColors.mango,
                                unfocusedBorderColor = funkyColors.line,
                                focusedTextColor = funkyColors.ink,
                                unfocusedTextColor = funkyColors.ink,
                                cursorColor = funkyColors.mango
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (inputText.isNotBlank() && !isSending) {
                                    val textToSend = inputText
                                    inputText = ""
                                    SupportChatManager.sendSupportMessage(
                                        context = context,
                                        messageText = textToSend,
                                        reportId = reportId,
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
                                        reportId = reportId,
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
                                .background(if (inputText.isNotBlank() && !isSending) funkyColors.mango else funkyColors.soft)
                                .testTag("support_chat_send_button")
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = funkyColors.onMango,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                FunkySendIcon(
                                    color = if (inputText.isNotBlank()) funkyColors.onMango else funkyColors.mute,
                                    modifier = Modifier.size(18.dp)
                                )
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
            if (messages.isEmpty() && !isLoading) {
                AppealEmptyChatState(
                    title = if (isAppeal) "Appeal" else "Support",
                    subtitle = if (isAppeal) "Tell us why this decision should be reviewed. Our team will reply here." else "Welcome! Send a message to get in touch with our team.",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                    color = funkyColors.mango
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
    val funkyColors = LocalFunkyColors.current
    val isDark = funkyColors.isDark
    val formattedUntil = remember(untilIso) { formatUntilTimeString(untilIso) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = funkyColors.soft,
        border = BorderStroke(1.dp, funkyColors.mango.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(funkyColors.mango.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                SupportClockGraphic(
                    color = if (isDark) funkyColors.mango else Color(0xFFD97706),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Our team has opened this chat for you. You can reply until $formattedUntil.",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = funkyColors.ink,
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
    val funkyColors = LocalFunkyColors.current
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .offset(x = 24.dp, y = 14.dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 28.dp, bottomEnd = 6.dp))
                    .background(funkyColors.soft)
            )
            Surface(
                modifier = Modifier
                    .size(105.dp)
                    .offset(x = (-12).dp, y = (-10).dp),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 8.dp, bottomEnd = 32.dp),
                color = funkyColors.mango,
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
                                .background(funkyColors.surface)
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
            color = funkyColors.ink,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = funkyColors.mute,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

/**
 * Full Screen Chat Ended State
 */
@Composable
private fun SupportChatEndedState(modifier: Modifier = Modifier) {
    val funkyColors = LocalFunkyColors.current
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier.size(110.dp),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomStart = 8.dp, bottomEnd = 32.dp),
                color = funkyColors.soft,
                border = BorderStroke(1.dp, funkyColors.line),
                shadowElevation = 4.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    SoftCloseIcon(
                        color = funkyColors.mute,
                        size = 42.dp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "This support chat has ended.",
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = funkyColors.ink,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "You can no longer send messages in this conversation.",
            fontSize = 14.sp,
            color = funkyColors.mute,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun SupportMessageBubble(message: SupabaseSupportMessage) {
    val funkyColors = LocalFunkyColors.current
    val isUser = message.sender_type == "user"
    val formattedTime = SupportChatManager.formatMessageTime(message.created_at)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(funkyColors.mango),
                contentAlignment = Alignment.Center
            ) {
                SoftChatBubbleIcon(
                    color = funkyColors.onMango,
                    size = 16.dp
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
                        color = funkyColors.mute
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
                color = if (isUser) funkyColors.mango else funkyColors.surface,
                border = BorderStroke(
                    1.dp,
                    if (isUser) funkyColors.mango else funkyColors.line
                ),
                shadowElevation = if (funkyColors.isDark) 0.dp else 0.5.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(
                        text = message.body,
                        fontSize = 14.sp,
                        color = if (isUser) funkyColors.onMango else funkyColors.ink,
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
                            color = if (isUser) funkyColors.onMango.copy(alpha = 0.75f) else funkyColors.mute
                        )
                        if (isUser) {
                            Spacer(modifier = Modifier.width(4.dp))
                            FunkySingleCheckIcon(
                                color = funkyColors.onMango.copy(alpha = 0.8f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportClockGraphic(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val sw = size.minDimension * 0.11f
        val r = size.minDimension * 0.38f
        val cx = size.width / 2f
        val cy = size.height / 2f
        drawCircle(color = color, radius = r, center = Offset(cx, cy), style = Stroke(width = sw))
        drawLine(color, Offset(cx, cy), Offset(cx, cy - r * 0.52f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, Offset(cx, cy), Offset(cx + r * 0.45f, cy + r * 0.15f), strokeWidth = sw, cap = StrokeCap.Round)
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
