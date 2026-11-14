package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.example.ui.components.SoftGiftBoxIcon
import com.example.ui.components.SoftPlusIcon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.SupabaseSessionManager
import com.example.SpeakerRequestInsert
import com.example.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

val globalFluentEmojis = listOf(
    "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Smiling%20face%20with%20hearts/3D/smiling_face_with_hearts_3d.png" to "🥰",
    "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Smiling%20face%20with%20sunglasses/3D/smiling_face_with_sunglasses_3d.png" to "😎",
    "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Fire/3D/fire_3d.png" to "🔥",
    "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Partying%20face/3D/partying_face_3d.png" to "🥳",
    "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Red%20heart/3D/red_heart_3d.png" to "❤️",
    "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Face%20with%20tears%20of%20joy/3D/face_with_tears_of_joy_3d.png" to "😂",
    "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Sparkles/3D/sparkles_3d.png" to "✨",
    "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Party%20popper/3D/party_popper_3d.png" to "🎉",
    "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Star-struck/3D/star-struck_3d.png" to "🤩",
    "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/Ghost/3D/ghost_3d.png" to "👻"
)

@Composable
fun RoomChatPanel(
    chatListState: LazyListState,
    renderedMessages: List<ChillRoomMessage>,
    messages: List<ChillRoomMessage>,
    renderedMessageCount: Int,
    onRenderedCountChange: (Int) -> Unit,
    currentTheme: RoomTheme,
    currentUserId: String,
    currentUserName: String,
    isSpeakerRequestSubmitting: Boolean,
    onSpeakerRequestSubmittingChange: (Boolean) -> Unit,
    isSpeakerRequestPending: Boolean,
    onSpeakerRequestPendingChange: (Boolean) -> Unit,
    activeRoomId: String,
    scope: CoroutineScope,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val inlineContentMap = remember {
        globalFluentEmojis.associate { (url, unicode) ->
            unicode to androidx.compose.foundation.text.InlineTextContent(
                androidx.compose.ui.text.Placeholder(
                    width = 24.sp,
                    height = 24.sp,
                    placeholderVerticalAlign = androidx.compose.ui.text.PlaceholderVerticalAlign.Center
                )
            ) {
                AsyncImage(
                    model = url,
                    contentDescription = unicode,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Auto-scroll to the newest message whenever new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            try {
                chatListState.animateScrollToItem(0)
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        LazyColumn(
            state = chatListState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            reverseLayout = true
        ) {
            items(
                items = renderedMessages,
                key = { it.id?.takeIf { k -> k.isNotBlank() } ?: "${it.userId}_${it.text}_${it.createdAt}_${it.messageType}_${it.hashCode()}" }
            ) { msg ->
                val isSystem = msg.messageType?.equals("system", ignoreCase = true) == true
                val isGift = msg.messageType?.equals("gift", ignoreCase = true) == true

                if (isGift) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (currentTheme.isDark) Color(0xFF2C2219).copy(alpha = 0.85f)
                                    else Color(0xFFFEF3C7).copy(alpha = 0.9f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (currentTheme.isDark) Color(0xFFFFB800).copy(alpha = 0.35f) else Color(0xFFFDE68A),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (!msg.iconUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = msg.iconUrl,
                                    contentDescription = "Gift",
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                SoftGiftBoxIcon(
                                    size = 16.dp,
                                    color = if (currentTheme.isDark) Color(0xFFFFD54F) else Color(0xFFD97706)
                                )
                            }

                            val boldColor = if (currentTheme.isDark) Color(0xFFFFD54F) else Color(0xFFD97706)
                            val textColor = if (currentTheme.isDark) Color(0xFFF8FAFC) else Color(0xFF1E293B)

                            val annotatedGiftText = buildAnnotatedString {
                                val fullText = msg.text
                                val sender = msg.senderNameBold
                                val recipient = msg.recipientNameBold

                                if (!sender.isNullOrBlank() && fullText.contains(sender)) {
                                    val sIdx = fullText.indexOf(sender)
                                    val beforeSender = fullText.substring(0, sIdx)
                                    withStyle(SpanStyle(color = textColor, fontWeight = FontWeight.Normal, fontSize = 12.sp)) {
                                        append(beforeSender)
                                    }
                                    withStyle(SpanStyle(color = boldColor, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)) {
                                        append(sender)
                                    }

                                    val afterSender = fullText.substring(sIdx + sender.length)
                                    if (!recipient.isNullOrBlank() && afterSender.contains(recipient)) {
                                        val rIdx = afterSender.indexOf(recipient)
                                        val between = afterSender.substring(0, rIdx)
                                        val afterRecipient = afterSender.substring(rIdx + recipient.length)

                                        withStyle(SpanStyle(color = textColor, fontWeight = FontWeight.Normal, fontSize = 12.sp)) {
                                            append(between)
                                        }
                                        withStyle(SpanStyle(color = boldColor, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)) {
                                            append(recipient)
                                        }
                                        withStyle(SpanStyle(color = textColor, fontWeight = FontWeight.Normal, fontSize = 12.sp)) {
                                            append(afterRecipient)
                                        }
                                    } else {
                                        withStyle(SpanStyle(color = textColor, fontWeight = FontWeight.Normal, fontSize = 12.sp)) {
                                            append(afterSender)
                                        }
                                    }
                                } else {
                                    withStyle(SpanStyle(color = textColor, fontWeight = FontWeight.Medium, fontSize = 12.sp)) {
                                        append(fullText)
                                    }
                                }
                            }

                            Text(
                                text = annotatedGiftText,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else if (isSystem) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (currentTheme.isDark) Color.White.copy(alpha = 0.08f)
                                    else Color.Black.copy(alpha = 0.05f)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = msg.text,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (currentTheme.isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                        if (msg.userId == currentUserId) {
                            Spacer(modifier = Modifier.width(6.dp))
                            // Small "+" / pending request-to-speak icon
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (isSpeakerRequestPending) Color(0xFF3F3F46) else ChillGoldIcon)
                                    .clickable(enabled = !isSpeakerRequestSubmitting) {
                                        if (isSpeakerRequestPending) {
                                            Toast.makeText(context, "Your request to speak is pending host approval", Toast.LENGTH_SHORT).show()
                                        } else {
                                            onSpeakerRequestSubmittingChange(true)
                                            val token = SupabaseSessionManager.getBearerToken(context)
                                            val uid = SupabaseSessionManager.getUserId(context) ?: currentUserId
                                            scope.launch(Dispatchers.IO) {
                                                try {
                                                    val res = SupabaseClient.api.createSpeakerRequest(
                                                        auth = token,
                                                        body = SpeakerRequestInsert(
                                                            room_id = activeRoomId,
                                                            user_id = uid,
                                                            status = "pending"
                                                        )
                                                    )
                                                    withContext(Dispatchers.Main) {
                                                        onSpeakerRequestSubmittingChange(false)
                                                        if (res.isSuccessful || res.code() == 409) {
                                                            onSpeakerRequestPendingChange(true)
                                                            Toast.makeText(context, "Request to speak sent to host!", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, "Could not submit speaker request.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    withContext(Dispatchers.Main) {
                                                        onSpeakerRequestSubmittingChange(false)
                                                        Toast.makeText(context, "Error sending request to speak", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSpeakerRequestPending) {
                                    CustomPendingHourglassIcon(
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                } else {
                                    SoftPlusIcon(
                                        size = 14.dp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        AsyncImage(
                            model = msg.senderImageUrl,
                            contentDescription = msg.senderName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = msg.senderName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = msg.badgeColor
                                )
                                if (msg.badge != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(msg.badgeColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = msg.badge,
                                            fontSize = 9.sp,
                                            color = msg.badgeColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = msg.text.toLocalFluentAnnotatedString(globalFluentEmojis),
                                inlineContent = inlineContentMap,
                                fontSize = 14.sp,
                                color = currentTheme.primaryTextColor,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }
            }

            if (renderedMessageCount < messages.size) {
                item(key = "load_older_cache_trigger") {
                    LaunchedEffect(Unit) {
                        if (renderedMessageCount < messages.size) {
                            onRenderedCountChange((renderedMessageCount + 6).coerceAtMost(messages.size))
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (renderedMessageCount < messages.size) {
                                    onRenderedCountChange((renderedMessageCount + 6).coerceAtMost(messages.size))
                                }
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val remaining = messages.size - renderedMessageCount
                        Text(
                            text = "↑ Scroll for older cached messages ($remaining)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = currentTheme.primaryTextColor.copy(alpha = 0.55f)
                        )
                    }
                }
            }
        }
    }
}

fun String.toLocalFluentAnnotatedString(fluentEmojis: List<Pair<String, String>>): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        var currentIndex = 0
        while (currentIndex < this@toLocalFluentAnnotatedString.length) {
            var matched = false
            for ((_, unicode) in fluentEmojis) {
                if (this@toLocalFluentAnnotatedString.startsWith(unicode, currentIndex)) {
                    appendInlineContent(unicode, unicode)
                    currentIndex += unicode.length
                    matched = true
                    break
                }
            }
            if (!matched) {
                val char = this@toLocalFluentAnnotatedString[currentIndex]
                if (char.isHighSurrogate() && currentIndex + 1 < this@toLocalFluentAnnotatedString.length) {
                    append(this@toLocalFluentAnnotatedString.substring(currentIndex, currentIndex + 2))
                    currentIndex += 2
                } else {
                    append(char)
                    currentIndex++
                }
            }
        }
    }
}
