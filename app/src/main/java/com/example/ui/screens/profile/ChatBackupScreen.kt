package com.example.ui.screens.profile

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBackupScreen(
    onBackClick: () -> Unit
) {
    com.example.ui.screens.voiceroom.ChatBackupScreenContent(
        onBack = onBackClick
    )
}
