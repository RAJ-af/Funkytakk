package com.example.ui.screens.profile

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AuthViewModel
import com.example.DeletionStatusResponse
import com.example.DeletionStatusState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AccountDeletionGateScreen(
    state: DeletionStatusState,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var isCancelling by remember { mutableStateOf(false) }

    // Intercept back button to log out instead of bypassing gate
    BackHandler {
        onLogout()
    }

    Scaffold(
        containerColor = Color(0xFFFFFDF5)
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
                        Icon(
                            imageVector = Icons.Default.HourglassEmpty,
                            contentDescription = "Pending Deletion",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(68.dp)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = if (canCancel) "Your account is scheduled for deletion" else "Your account is being deleted",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (!info.receipt_id.isNullOrBlank()) {
                            Text(
                                text = "Receipt: ${info.receipt_id}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B)
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
                            color = Color(0xFF475569),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Log out", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                        }
                    }
                }
                is DeletionStatusState.Failed -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(68.dp)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Unable to verify account status",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = state.error.ifBlank { "Could not connect to verify account deletion state. Please check your network and retry." },
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = {
                                authViewModel.checkDeletionStatus(context)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Retry", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = onLogout,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Log out", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                        }
                    }
                }
                else -> {
                    CircularProgressIndicator(color = Color(0xFFF59E0B))
                }
            }
        }
    }
}
