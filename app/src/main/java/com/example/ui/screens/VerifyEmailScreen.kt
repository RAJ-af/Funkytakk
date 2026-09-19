package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AuthState
import com.example.AuthViewModel

@Composable
fun VerifyEmailScreen(
    viewModel: AuthViewModel,
    onVerifiedSuccess: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()

    var userEmail by remember { mutableStateOf("your email") }
    var isVerified by remember { mutableStateOf(false) }

    var cooldownSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            kotlinx.coroutines.delay(1000)
            cooldownSeconds -= 1
        }
    }

    DisposableEffect(Unit) {
        viewModel.startAutoVerificationCheck(context) {
            onVerifiedSuccess()
        }
        onDispose {
            viewModel.stopAutoVerificationCheck()
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            val user = (authState as AuthState.Success).user
            userEmail = user.email
            if (user.isEmailVerified) {
                isVerified = true
                onVerifiedSuccess()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Large styled mail icon
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFFFF4D4)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isVerified) Icons.Default.MarkEmailRead else Icons.Default.Email,
                        contentDescription = "Mail Verification pending",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Verify your email",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("verify_email_header")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "We have sent a verification link to:\n$userEmail\n\nPlease check your inbox or open directly in your browser to verify your account.",
                    fontSize = 14.sp,
                    color = Color(0xFF6B6E7B),
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Beautiful interactive status check card with automated loading state
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9FB)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFFF9E00),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Awaiting verification...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                            Text(
                                text = "Auto-checking every few seconds.",
                                fontSize = 12.sp,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Open in External Browser (Chrome) via ACTION_VIEW Intent
                Button(
                    onClick = {
                        viewModel.openVerificationLinkInBrowser(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("open_browser_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF141414),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = "Browser",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Open Link in Browser",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Open Email Inbox
                OutlinedButton(
                    onClick = {
                        viewModel.openEmailApp(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("open_email_app_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF141414)
                    ),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email App",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "Open Email Inbox",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Check Verification Link Status Now Button
                Button(
                    onClick = {
                        viewModel.checkAndRefreshVerification(context) {
                            onVerifiedSuccess()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("verify_otp_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9E00),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "I have clicked the link",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Resend email link with 60s cooldown indicator
                Button(
                    onClick = {
                        if (cooldownSeconds == 0) {
                            cooldownSeconds = 60
                            viewModel.resendVerificationEmail(context)
                        }
                    },
                    enabled = cooldownSeconds == 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("resend_email_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (cooldownSeconds == 0) Color(0xFFF5F5F7) else Color(0xFFF1F5F9),
                        contentColor = if (cooldownSeconds == 0) Color.Black else Color.Gray,
                        disabledContainerColor = Color(0xFFF1F5F9),
                        disabledContentColor = Color.Gray
                    ),
                    shape = RoundedCornerShape(26.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = if (cooldownSeconds > 0) "Resend link in ${cooldownSeconds}s" else "Resend Verification Link",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Back to login/registration screen trigger
                TextButton(
                    onClick = {
                        viewModel.logout(context)
                        onBackClick()
                    },
                    modifier = Modifier.testTag("back_to_login_button")
                ) {
                    Text(
                        text = "Back to Sign Up / Log In",
                        color = Color(0xFFFF9E00),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
