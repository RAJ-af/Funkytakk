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
    val lang = contact.isNativeLanguage.lowercase()
    val flagCode = when {
        lang.contains("jp") || lang.contains("japan") -> "jp"
        lang.contains("kr") || lang.contains("korea") -> "kr"
        lang.contains("cn") || lang.contains("china") || lang.contains("chinese") -> "cn"
        lang.contains("es") || lang.contains("spain") || lang.contains("spanish") -> "es"
        lang.contains("de") || lang.contains("german") -> "de"
        lang.contains("fr") || lang.contains("french") -> "fr"
        lang.contains("in") || lang.contains("hindi") || lang.contains("india") -> "in"
        lang.contains("ru") || lang.contains("russian") -> "ru"
        lang.contains("en") || lang.contains("english") || lang.contains("us") -> "us"
        else -> "jp"
    }

    val countryName = when (flagCode) {
        "jp" -> "Japan"
        "kr" -> "South Korea"
        "cn" -> "China"
        "es" -> "Spain"
        "de" -> "Germany"
        "fr" -> "France"
        "in" -> "India"
        "ru" -> "Russia"
        "us" -> "United States"
        else -> "Japan"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAEE))
    ) {
        DirectChatScreen(
            partnerName = if (contact.name.isNotBlank()) contact.name else "Yuki Tanaka",
            partnerAvatarUrl = if (contact.avatar.isNotBlank()) contact.avatar else "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
            partnerCountry = countryName,
            partnerCountryCode = flagCode,
            onBack = onDismiss
        )
    }
}
