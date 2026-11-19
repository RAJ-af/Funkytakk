package com.example.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalFunkyColors

@Composable
fun BlockUserConfirmDialog(
    displayName: String,
    isBlocking: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    val safeName = displayName.ifBlank { "this user" }

    AlertDialog(
        onDismissRequest = { if (!isBlocking) onDismiss() },
        containerColor = funkyColors.surface,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = "Block $safeName?",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = funkyColors.ink
            )
        },
        text = {
            Text(
                text = "Block $safeName? They will not be able to message you, and you will not see their posts.",
                fontSize = 14.sp,
                color = funkyColors.mute,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isBlocking,
                colors = ButtonDefaults.buttonColors(
                    containerColor = funkyColors.danger,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("confirm_block_user_button")
            ) {
                if (isBlocking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (isBlocking) "Blocking..." else "Block",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isBlocking,
                modifier = Modifier.testTag("cancel_block_user_button")
            ) {
                Text(
                    text = "Cancel",
                    color = funkyColors.ink,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}
