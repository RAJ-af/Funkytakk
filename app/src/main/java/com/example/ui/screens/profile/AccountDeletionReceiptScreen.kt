package com.example.ui.screens.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AuthViewModel
import com.example.DeletionReceiptData
import com.example.ui.components.SoftCheckmarkBadge
import com.example.ui.components.SoftCopyIcon
import com.example.ui.components.SoftShareIcon
import com.example.ui.theme.LocalFunkyColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AccountDeletionReceiptScreen(
    receipt: DeletionReceiptData,
    authViewModel: AuthViewModel,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val funkyColors = LocalFunkyColors.current
    val isDark = funkyColors.isDark
    var isFinishing by remember { mutableStateOf(false) }

    fun finishAndSignOut() {
        if (isFinishing) return
        isFinishing = true
        authViewModel.finalizeDeletionAndSignOut(context) {
            onDone()
        }
    }

    // Intercept Back button so user cannot return to the deletion form
    BackHandler {
        finishAndSignOut()
    }

    val formattedDate = remember(receipt.scheduledFor) {
        if (receipt.scheduledFor.isBlank()) {
            "Within ${receipt.graceDays} days"
        } else {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = inputFormat.parse(receipt.scheduledFor.take(19))
                if (date != null) {
                    val outputFormat = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
                    outputFormat.format(date)
                } else {
                    receipt.scheduledFor
                }
            } catch (_: Exception) {
                receipt.scheduledFor
            }
        }
    }

    Scaffold(
        containerColor = funkyColors.bg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SoftCheckmarkBadge(
                size = 72.dp,
                badgeColor = funkyColors.ok,
                checkColor = Color.White
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Deletion Scheduled",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = funkyColors.ink
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your account has been scheduled for permanent deletion.",
                fontSize = 13.sp,
                color = funkyColors.mute,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Receipt Box
            Surface(
                color = funkyColors.surface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, funkyColors.line),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "RECEIPT NUMBER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = funkyColors.mute,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SelectionContainer {
                        Text(
                            text = receipt.receiptId,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.ink
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Deletion Receipt", receipt.receiptId)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Receipt copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, funkyColors.line),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = funkyColors.ink)
                        ) {
                            SoftCopyIcon(size = 16.dp, color = funkyColors.ink)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy", fontSize = 12.sp, color = funkyColors.ink)
                        }

                        OutlinedButton(
                            onClick = {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "FunkyTalk Deletion Receipt: ${receipt.receiptId}\nScheduled deletion: $formattedDate")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Receipt"))
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, funkyColors.line),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = funkyColors.ink)
                        ) {
                            SoftShareIcon(size = 16.dp, color = funkyColors.ink)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share", fontSize = 12.sp, color = funkyColors.ink)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = funkyColors.line)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "PERMANENT PURGE DATE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = funkyColors.mute,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = formattedDate,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = funkyColors.danger
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Notice
            Surface(
                color = if (isDark) funkyColors.soft else Color(0xFFEFF6FF),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isDark) funkyColors.line else Color(0xFFBFDBFE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "We also emailed you this receipt. To cancel, sign in before that date.",
                    fontSize = 12.sp,
                    color = if (isDark) funkyColors.ink else Color(0xFF1D4ED8),
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Done Button
            Button(
                onClick = { finishAndSignOut() },
                enabled = !isFinishing,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = funkyColors.mango,
                    contentColor = funkyColors.onMango
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isFinishing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = funkyColors.onMango)
                } else {
                    Text("Done", color = funkyColors.onMango, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
