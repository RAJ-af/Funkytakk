package com.example.ui.screens.profile

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AuthViewModel
import com.example.DeletionStatusState
import com.example.ui.components.SoftWarningIcon
import com.example.ui.theme.LocalFunkyColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AccountDeletionGateScreen(
    state: DeletionStatusState,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val funkyColors = LocalFunkyColors.current
    var isCancelling by remember { mutableStateOf(false) }

    // Intercept back button to log out instead of bypassing gate
    BackHandler {
        onLogout()
    }

    Scaffold(
        containerColor = funkyColors.bg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is DeletionStatusState.Pending -> {
                    val info = state.info
                    val canCancel = info.can_cancel != false && info.status != "purging"

                    val formattedDate = remember(info.scheduled_for) {
                        if (info.scheduled_for.isNullOrBlank()) {
                            "Soon"
                        } else {
                            try {
                                val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                                    timeZone = TimeZone.getTimeZone("UTC")
                                }
                                val date = inputFormat.parse(info.scheduled_for.take(19))
                                if (date != null) {
                                    val outputFormat = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.getDefault())
                                    outputFormat.format(date)
                                } else {
                                    info.scheduled_for
                                }
                            } catch (_: Exception) {
                                info.scheduled_for
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PendingHourglassGraphic(
                            color = funkyColors.mango,
                            modifier = Modifier.size(68.dp)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = if (canCancel) "Your account is scheduled for deletion" else "Your account is being deleted",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.ink,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (!info.receipt_id.isNullOrBlank()) {
                            Text(
                                text = "Receipt: ${info.receipt_id}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = funkyColors.mute
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Text(
                            text = if (canCancel) {
                                "Permanent purge scheduled for:\n$formattedDate\n\nIf you want to keep your account, you can cancel deletion now."
                            } else {
                                "Your account deletion is currently being processed by the system. All data and login access are being permanently wiped."
                            },
                            fontSize = 13.sp,
                            color = funkyColors.mute,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        if (canCancel) {
                            Button(
                                onClick = {
                                    isCancelling = true
                                    authViewModel.cancelAccountDeletion(context) { success, errorMsg ->
                                        isCancelling = false
                                        if (!success) {
                                            Toast.makeText(context, errorMsg ?: "Failed to cancel deletion", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = !isCancelling,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = funkyColors.ok),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                            ) {
                                if (isCancelling) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                } else {
                                    Text("Cancel deletion", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        OutlinedButton(
                            onClick = onLogout,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, funkyColors.line),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = funkyColors.ink),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Log out", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = funkyColors.ink)
                        }
                    }
                }
                is DeletionStatusState.Failed -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SoftWarningIcon(
                            color = funkyColors.danger,
                            size = 64.dp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Unable to verify account status",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.ink,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = state.error.ifBlank { "Could not connect to verify account deletion state. Please check your network and retry." },
                            fontSize = 13.sp,
                            color = funkyColors.mute,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = {
                                authViewModel.checkDeletionStatus(context)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = funkyColors.mango,
                                contentColor = funkyColors.onMango
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Retry", color = funkyColors.onMango, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = onLogout,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, funkyColors.line),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = funkyColors.ink),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Log out", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = funkyColors.ink)
                        }
                    }
                }
                else -> {
                    CircularProgressIndicator(color = funkyColors.mango)
                }
            }
        }
    }
}

@Composable
private fun PendingHourglassGraphic(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val sw = w * 0.08f
        drawLine(color, Offset(w * 0.24f, h * 0.14f), Offset(w * 0.76f, h * 0.14f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.24f, h * 0.86f), Offset(w * 0.76f, h * 0.86f), strokeWidth = sw, cap = StrokeCap.Round)
        val glass = Path().apply {
            moveTo(w * 0.30f, h * 0.16f)
            lineTo(w * 0.30f, h * 0.34f)
            lineTo(w * 0.50f, h * 0.50f)
            lineTo(w * 0.70f, h * 0.34f)
            lineTo(w * 0.70f, h * 0.16f)
            moveTo(w * 0.30f, h * 0.84f)
            lineTo(w * 0.30f, h * 0.66f)
            lineTo(w * 0.50f, h * 0.50f)
            lineTo(w * 0.70f, h * 0.66f)
            lineTo(w * 0.70f, h * 0.84f)
        }
        drawPath(glass, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
