package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailRead
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Icon and instruction text block
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Large styled mail icon
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFFFF4D4)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isVerified) Icons.Default.MarkEmailRead else Icons.Default.Email,
                        contentDescription = "Mail Verification pending",
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Verify your email",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("verify_email_header")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "We have sent a secure verification link to:\n$userEmail\n\nPlease check your email inbox and click the link to verify your account.",
                    fontSize = 15.sp,
                    color = Color(0xFF6B6E7B),
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Beautiful interactive status check card with automated loading state
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9FB)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFFF9E00),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Awaiting email click...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                            Text(
                                text = "We are checking automatically.",
                                fontSize = 12.sp,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }
                }
            }

            // Buttons controller actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Check Verification Link Status Now Button
                Button(
                    onClick = {
                        viewModel.checkAndRefreshVerification(context) {
                            onVerifiedSuccess()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("verify_otp_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF9E00),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "I have clicked the link",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Resend email link
                Button(
                    onClick = {
                        viewModel.resendVerificationEmail(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("resend_email_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF5F5F7),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(28.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = "Resend Verification Link",
                        fontSize = 15.sp,
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
