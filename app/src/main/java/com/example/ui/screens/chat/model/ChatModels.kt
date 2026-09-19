package com.example.ui.screens.chat.model

enum class ChatMessageType {
    TEXT,
    AUDIO,
    IMAGE
}

data class ChatMessage(
    val id: String,
    val text: String = "",
    val time: String,
    val isMe: Boolean,
    val type: ChatMessageType = ChatMessageType.TEXT,
    val imageUrl: String? = null,
    val audioDuration: String = "0:12",
    val audioDurationSecs: Int = 12,
    val reactions: List<String> = emptyList(),
    val isTranslated: Boolean = false,
    val translation: String? = null,
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val showAvatar: Boolean = true
)

object ChatSampleData {
    fun getInitialMessages(): List<ChatMessage> {
        return listOf(
            ChatMessage(
                id = "m1",
                text = "Hello! Glad we connected here. 😊",
                time = "9:12 AM",
                isMe = false,
                type = ChatMessageType.TEXT,
                showAvatar = true
            ),
            ChatMessage(
                id = "m2",
                text = "Your Japanese is improving so fast!",
                time = "9:12 AM",
                isMe = false,
                type = ChatMessageType.TEXT,
                reactions = listOf("❤️"),
                showAvatar = true
            ),
            ChatMessage(
                id = "m3",
                text = "Thank you! 😊\nI'm trying my best!",
                time = "9:14 AM",
                isMe = true,
                type = ChatMessageType.TEXT
            ),
            ChatMessage(
                id = "m4",
                text = "Do you want to practice a little now? We can talk about travel in Japan! 🇯🇵",
                time = "9:15 AM",
                isMe = false,
                type = ChatMessageType.TEXT,
                showAvatar = true
            ),
            ChatMessage(
                id = "m5",
                text = "Yes! That sounds great!",
                time = "9:16 AM",
                isMe = true,
                type = ChatMessageType.TEXT
            ),
            ChatMessage(
                id = "m6",
                time = "9:17 AM",
                isMe = false,
                type = ChatMessageType.AUDIO,
                audioDuration = "0:12",
                audioDurationSecs = 12,
                showAvatar = true
            ),
            ChatMessage(
                id = "m7",
                time = "9:18 AM",
                isMe = false,
                type = ChatMessageType.IMAGE,
                imageUrl = "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=800",
                reactions = listOf("❤️", "❤️"),
                showAvatar = false
            ),
            ChatMessage(
                id = "m8",
                text = "This is from my last trip!\nHave you been to Japan?",
                time = "9:18 AM",
                isMe = false,
                type = ChatMessageType.TEXT,
                showAvatar = true
            ),
            ChatMessage(
                id = "m9",
                text = "Wow! Amazing 😍\nI haven't been yet, but it's on my list!",
                time = "9:20 AM",
                isMe = true,
                type = ChatMessageType.TEXT
            )
        )
    }
}
