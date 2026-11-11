package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ModerationManager
import com.example.R
import com.example.ui.components.SoftHeadsetIcon
import com.example.ui.components.SoftShieldIcon
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.*

@Composable
fun AccountUnderReviewScreen(
    onContactSupport: () -> Unit,
    onReviewEnded: () -> Unit = {}
) {
    val context = LocalContext.current
    var showExplanationDialog by remember { mutableStateOf(true) }
    var isChecking by remember { mutableStateOf(false) }

    // Disable system back button to keep app strictly locked
    BackHandler(enabled = true) {
        // Back press does not exit lock screen
    }

    // Poll every 60 seconds while locked
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(60_000L)
            try {
                isChecking = true
                val res = ModerationManager.fetchMyAccountReview(context)
                val isUnderReview = res.getOrNull()?.under_review == true
                if (!isUnderReview) {
                    Toast.makeText(context, "Your account review is complete", Toast.LENGTH_LONG).show()
                    onReviewEnded()
                    break
                }
            } catch (_: Exception) {
            } finally {
                isChecking = false
            }
        }
    }

    // Modal dialog from design screenshot (Bunny mascot, shield, yellow button)
    if (showExplanationDialog) {
        Dialog(
            onDismissRequest = { /* Modal: user must acknowledge */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Bunny mascot with shield
                        Box(
                            modifier = Modifier
                                .size(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ReviewBunnyShieldMascot(modifier = Modifier.fillMaxSize())
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Title
                        Text(
                            text = "Your Account is Under Review",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Body
                        Text(
                            text = "You can't access your account right now. Someone has reported your account, and our team is verifying it according to our community guidelines.",
                            fontSize = 13.5.sp,
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center,
                            lineHeight = 19.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Note card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFFBEB),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SoftShieldIcon(size = 18.dp, color = Color(0xFFD97706))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Our team is currently checking your account. We'll notify you once the review is complete.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E),
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Yellow acknowledge button
                        Button(
                            onClick = {
                                showExplanationDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFD200),
                                contentColor = Color(0xFF1E293B)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = "Okay, I Understand",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Persistent locked screen (Shown underneath and after dismissing explanation)
    Scaffold(
        containerColor = Color(0xFFFCFBF7)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 28.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Big mascot graphic
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                ReviewBunnyShieldMascot(modifier = Modifier.fillMaxSize())
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Your Account is Under Review",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Our moderation team is reviewing your account activity in accordance with our community safety standards.",
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFEF3C7),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCD34D)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "What happens next?",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• The review usually takes less than 24 hours.\n• You will automatically be unlocked once completed.\n• If needed, our team will message you via Support Chat.",
                        fontSize = 12.5.sp,
                        color = Color(0xFF78350F),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            val supportSession by com.example.SupportChatManager.session.collectAsState()
            val isSupportAvailable = supportSession?.can_appeal == true || (supportSession?.open == true && !com.example.SupportChatManager.isSessionExpired(supportSession))

            if (isSupportAvailable) {
                Button(
                    onClick = onContactSupport,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFD200),
                        contentColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    SoftHeadsetIcon(size = 20.dp, color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (supportSession?.can_appeal == true || supportSession?.reason == "appeal") "Appeal this decision" else "Open support chat",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SoftShieldIcon(size = 20.dp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Our team will message you here if they need anything.",
                            fontSize = 13.sp,
                            color = Color(0xFF475569),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Check review status button
            TextButton(
                onClick = {
                    isChecking = true
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                        try {
                            val res = ModerationManager.fetchMyAccountReview(context)
                            val isUnderReview = res.getOrNull()?.under_review == true
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                isChecking = false
                                if (!isUnderReview) {
                                    Toast.makeText(context, "Your account review is complete", Toast.LENGTH_LONG).show()
                                    onReviewEnded()
                                } else {
                                    Toast.makeText(context, "Review in progress. Please check back shortly.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (_: Exception) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                isChecking = false
                            }
                        }
                    }
                },
                enabled = !isChecking
            ) {
                if (isChecking) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF64748B))
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = if (isChecking) "Checking..." else "Refresh Status",
                    color = Color(0xFF64748B),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Cute vector bunny mascot with protective shield
 */
@Composable
fun ReviewBunnyShieldMascot(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Background soft gradient aura
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFEF08A).copy(alpha = 0.5f), Color.Transparent)
                    )
                )
        )

        // Mascot graphic with shield overlay
        Canvas(modifier = Modifier.size(110.dp)) {
            val w = size.width
            val h = size.height

            // Bunny Ears
            // Left ear
            drawOval(
                brush = Brush.verticalGradient(listOf(Color(0xFFFFE4E6), Color(0xFFFDA4AF))),
                topLeft = Offset(w * 0.24f, h * 0.05f),
                size = androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.42f)
            )
            // Left inner ear
            drawOval(
                color = Color(0xFFF43F5E).copy(alpha = 0.5f),
                topLeft = Offset(w * 0.28f, h * 0.12f),
                size = androidx.compose.ui.geometry.Size(w * 0.10f, h * 0.26f)
            )

            // Right ear
            drawOval(
                brush = Brush.verticalGradient(listOf(Color(0xFFFFE4E6), Color(0xFFFDA4AF))),
                topLeft = Offset(w * 0.58f, h * 0.05f),
                size = androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.42f)
            )
            // Right inner ear
            drawOval(
                color = Color(0xFFF43F5E).copy(alpha = 0.5f),
                topLeft = Offset(w * 0.62f, h * 0.12f),
                size = androidx.compose.ui.geometry.Size(w * 0.10f, h * 0.26f)
            )

            // Bunny Head
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFFFF1F2)),
                    center = Offset(w * 0.5f, h * 0.52f),
                    radius = w * 0.35f
                ),
                radius = w * 0.32f,
                center = Offset(w * 0.5f, h * 0.52f)
            )

            // Cheeks
            drawCircle(
                color = Color(0xFFFB7185).copy(alpha = 0.35f),
                radius = w * 0.06f,
                center = Offset(w * 0.32f, h * 0.57f)
            )
            drawCircle(
                color = Color(0xFFFB7185).copy(alpha = 0.35f),
                radius = w * 0.06f,
                center = Offset(w * 0.68f, h * 0.57f)
            )

            // Eyes
            drawCircle(
                color = Color(0xFF1E293B),
                radius = w * 0.038f,
                center = Offset(w * 0.40f, h * 0.48f)
            )
            drawCircle(
                color = Color.White,
                radius = w * 0.015f,
                center = Offset(w * 0.39f, h * 0.47f)
            )

            drawCircle(
                color = Color(0xFF1E293B),
                radius = w * 0.038f,
                center = Offset(w * 0.60f, h * 0.48f)
            )
            drawCircle(
                color = Color.White,
                radius = w * 0.015f,
                center = Offset(w * 0.59f, h * 0.47f)
            )

            // Nose
            drawOval(
                color = Color(0xFFE11D48),
                topLeft = Offset(w * 0.47f, h * 0.53f),
                size = androidx.compose.ui.geometry.Size(w * 0.06f, h * 0.04f)
            )

            // Mouth
            val mouthPath = Path().apply {
                moveTo(w * 0.44f, h * 0.59f)
                quadraticBezierTo(w * 0.50f, h * 0.63f, w * 0.56f, h * 0.59f)
            }
            drawPath(
                path = mouthPath,
                color = Color(0xFF94A3B8),
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )

            // Shield in front of bunny
            val shieldPath = Path().apply {
                moveTo(w * 0.5f, h * 0.62f)
                lineTo(w * 0.72f, h * 0.66f)
                quadraticBezierTo(w * 0.72f, h * 0.88f, w * 0.5f, h * 0.98f)
                quadraticBezierTo(w * 0.28f, h * 0.88f, w * 0.28f, h * 0.66f)
                close()
            }

            // Shield shadow & fill
            drawPath(
                path = shieldPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFDE047), Color(0xFFEAB308), Color(0xFFCA8A04))
                )
            )
            drawPath(
                path = shieldPath,
                color = Color(0xFFFEF9C3),
                style = Stroke(width = 3f)
            )

            // Checkmark inside shield
            val checkPath = Path().apply {
                moveTo(w * 0.41f, h * 0.79f)
                lineTo(w * 0.47f, h * 0.85f)
                lineTo(w * 0.60f, h * 0.73f)
            }
            drawPath(
                path = checkPath,
                color = Color(0xFF78350F),
                style = Stroke(width = 4.5f, cap = StrokeCap.Round)
            )
        }
    }
}
