package com.example.ui.screens.home.dialogs

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.LocalFunkyColors

@Composable
fun LogoutConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = funkyColors.surface,
        title = { Text(text = "Sign Out", fontWeight = FontWeight.Bold, color = funkyColors.ink) },
        text = { Text(text = "Are you sure you want to sign out from FunkyTalk?", color = funkyColors.mute) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = funkyColors.mango,
                    contentColor = funkyColors.onMango
                )
            ) {
                Text("Confirm", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = funkyColors.mute)
            }
        }
    )
}
