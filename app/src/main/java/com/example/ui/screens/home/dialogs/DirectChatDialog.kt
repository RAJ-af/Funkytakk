package com.example.ui.screens.home.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.screens.chat.DirectChatScreen
import com.example.ui.screens.home.models.DirectChat

@Composable
fun DirectChatDialog(
    contact: DirectChat,
    messages: List<Pair<String, Boolean>>,
    onDismiss: () -> Unit,
    onSendMessage: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        DirectChatScreen(
            partnerName = if (contact.name.isNotBlank()) contact.name else "Yuki Tanaka",
            partnerAvatarUrl = if (contact.avatar.isNotBlank()) contact.avatar else "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
            partnerCountry = if (contact.isNativeLanguage.contains("JP", ignoreCase = true) || contact.isNativeLanguage.contains("Japanese", ignoreCase = true)) "Japan" else "Japan",
            onBack = onDismiss
        )
    }
}
