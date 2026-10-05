package com.example.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Debug-only screen showing every component with fake data. Mount it from a hidden debug menu and check
 * it in light + dark and in all four ChatPreset values before wiring real data.
 */
@Composable
fun ChatKitGallery(darkTheme: Boolean, preset: ChatPreset = ChatPreset.Butter, textScale: Float = 1f) {
    ProvideChatTheme(darkTheme = darkTheme, preset = preset, textScale = textScale) {
        val c = LocalChatColors.current
        var text by remember { mutableStateOf("") }
        val amps = remember { listOf(30, 60, 110, 70, 140, 100, 60, 120, 180, 90, 50, 110, 160, 130, 80, 150, 200, 100, 60, 130, 170, 90, 50, 100) }
        val edge = ChatDimens.ScreenEdge
        Column(Modifier.fillMaxSize().background(c.wallpaper)) {
            PinnedMessageBar("Practice at 8 pm today", "1 of 2", onClick = {})
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = edge),
                verticalArrangement = Arrangement.spacedBy(ChatDimens.InGroupGap),
            ) {
                SecurityNotice("Messages are end-to-end encrypted. Only people in this chat can read them.")
                DateChip("Today")
                SystemEventLine("Hacker updated group settings \u00B7 3 changes")
                UnreadDivider(2)

                // incoming group (first + last), mention, reaction, translation
                Row(horizontalArrangement = Arrangement.spacedBy(ChatDimens.AvatarGap)) {
                    Box(Modifier.size(ChatDimens.AvatarSize))
                    MessageBubble(outgoing = false, position = GroupPosition.First) {
                        SenderName("Hacker", "u1")
                        MessageText(
                            text = buildMessageText("@Arrav kal ka practice kaisa raha?", listOf("Arrav"), c.accentIn),
                            time = "12:51", outgoing = false, status = null,
                            onWordClick = {},
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(ChatDimens.AvatarGap), verticalAlignment = Alignment.Bottom) {
                    Box(Modifier.size(ChatDimens.AvatarSize).clip(CircleShape).background(c.senderColor("u1")))
                    MessageBubble(outgoing = false, position = GroupPosition.Last) {
                        MessageText(
                            text = androidx.compose.ui.text.AnnotatedString("Hindi me bolo, phir English me"),
                            time = "12:51", outgoing = false, status = null,
                        )
                        TranslationStrip("Speak in Hindi first, then in English", "English", outgoing = false, viaCloud = true, onListen = {})
                    }
                }
                ReactionRow(
                    listOf(ReactionUi("\u2764\uFE0F", 2, false), ReactionUi("\uD83D\uDC4D", 1, true)),
                    onClick = {}, modifier = Modifier.padding(start = ChatDimens.AvatarSize + ChatDimens.AvatarGap),
                )

                // outgoing reply
                Row(Modifier.fillMaxWidth().padding(top = ChatDimens.BetweenGroupsGap), horizontalArrangement = Arrangement.End) {
                    MessageBubble(outgoing = true, position = GroupPosition.Single) {
                        ReplyQuote("Hacker", "@Arrav kal ka practice kaisa raha?", outgoing = true, onClick = {})
                        MessageText(
                            text = androidx.compose.ui.text.AnnotatedString("Bahut accha tha, naye words seekhe"),
                            time = "12:52", outgoing = true, status = MessageStatus.Read,
                        )
                    }
                }

                // voice notes
                Row(Modifier.padding(top = ChatDimens.BetweenGroupsGap), horizontalArrangement = Arrangement.spacedBy(ChatDimens.AvatarGap)) {
                    Box(Modifier.size(ChatDimens.AvatarSize))
                    MessageBubble(outgoing = false, position = GroupPosition.Single) {
                        VoiceNoteContent(
                            outgoing = false, amplitudes = amps, durationMs = 12_000, positionMs = 4_000,
                            isPlaying = true, isLoading = false, isUnread = false, speed = 1.5f,
                            onPlayPause = {}, onSeek = {}, onSpeedClick = {},
                            transcript = "Aaj ka topic: travel",
                        )
                        MessageMeta("01:00", outgoing = false, status = null, modifier = Modifier.align(Alignment.End))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = ChatDimens.BetweenGroupsGap), horizontalArrangement = Arrangement.End) {
                    MessageBubble(outgoing = true, position = GroupPosition.Single) {
                        VoiceNoteContent(
                            outgoing = true, amplitudes = amps, durationMs = 12_000, positionMs = 0,
                            isPlaying = false, isLoading = false, isUnread = true, speed = 1f,
                            onPlayPause = {}, onSeek = {}, onSpeedClick = {},
                        )
                        MessageMeta("01:02", outgoing = true, status = MessageStatus.Delivered, modifier = Modifier.align(Alignment.End))
                    }
                }

                // image with overlay time (no caption)
                Row(Modifier.fillMaxWidth().padding(top = ChatDimens.BetweenGroupsGap), horizontalArrangement = Arrangement.End) {
                    MessageBubble(outgoing = true, position = GroupPosition.Single, contentPadding = PaddingValues(ChatDimens.MediaPad)) {
                        MediaFrame(
                            aspectRatio = 4f / 3f,
                            modifier = Modifier.size(width = 240.dp, height = 180.dp),
                            overlay = { MediaTimeChip("12:56", true, MessageStatus.Sent, Modifier.align(Alignment.BottomEnd).padding(8.dp)) },
                        ) { Box(Modifier.fillMaxSize().background(c.senderColor("photo"))) }
                    }
                }

                // link preview + file
                Row(Modifier.fillMaxWidth().padding(top = ChatDimens.BetweenGroupsGap), horizontalArrangement = Arrangement.End) {
                    MessageBubble(outgoing = true, position = GroupPosition.Single) {
                        LinkPreviewCard("funkytalk.web.app", "Join my FunkyTalk group", "Language practice and casual conversations", outgoing = true, onClick = {})
                        MessageText(
                            text = androidx.compose.ui.text.AnnotatedString("https://funkytalk-f1aae.web.app/g/abc"),
                            time = "12:57", outgoing = true, status = MessageStatus.Sent,
                        )
                    }
                }
                Row(Modifier.padding(top = ChatDimens.BetweenGroupsGap), horizontalArrangement = Arrangement.spacedBy(ChatDimens.AvatarGap)) {
                    Box(Modifier.size(ChatDimens.AvatarSize))
                    MessageBubble(outgoing = false, position = GroupPosition.Single) {
                        FileCard("PDF", "Lesson notes week 3.pdf", "1.2 MB", outgoing = false, onClick = {})
                        MessageMeta("12:58", outgoing = false, status = null, modifier = Modifier.align(Alignment.End))
                    }
                }

                // emoji only
                Row(Modifier.fillMaxWidth().padding(top = ChatDimens.BetweenGroupsGap), horizontalArrangement = Arrangement.End) {
                    EmojiOnlyMessage("\uD83D\uDE02\uD83D\uDE02", emojiOnlyCount("\uD83D\uDE02\uD83D\uDE02").coerceAtLeast(2), "12:59", true, MessageStatus.Read)
                }
                TypingIndicator("Hacker is typing")
                Box(Modifier.height(8.dp))
            }
            ChatInputBar(
                text = text, onTextChange = { text = it }, onSend = { text = "" }, onAttach = {}, onEmoji = {},
                showAiButton = true, onAi = {},
                micButton = { MicHoldButton(isRecording = false, onStart = {}, onMove = { _, _ -> }, onEnd = { _, _ -> }) },
            )
        }
    }
}
