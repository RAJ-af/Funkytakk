package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AuthState
import com.example.AuthViewModel
import com.example.R
import com.example.BuildConfig
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    onVerificationWaiting: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID
    val isRealGoogleAuthAvailable = webClientId.isNotEmpty() && webClientId != "YOUR_GOOGLE_WEB_CLIENT_ID"

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            val idToken = account.idToken
            if (idToken != null) {
                viewModel.loginWithGoogle(context, idToken, onAuthSuccess)
            } else {
                Toast.makeText(context, "Google didn't share your auth token, bestie! Try again.", Toast.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            val friendlyMsg = when (e.statusCode) {
                CommonStatusCodes.DEVELOPER_ERROR -> { // Code 10
                    "Google setup needs SHA-1 fingerprint & Web Client ID synced in Firebase Console! Use Email login in the meantime, bestie."
                }
                CommonStatusCodes.SIGN_IN_REQUIRED -> {
                    "Google sign-in needed! Pick your account to roll."
                }
                CommonStatusCodes.NETWORK_ERROR -> {
                    "Network's ghosting us! Check your connection, bestie."
                }
                CommonStatusCodes.CANCELED -> {
                    "Sign-in cancelled! Whenever you're ready, tap continue."
                }
                else -> {
                    "Google Sign-In hit a glitch (${e.statusCode}): ${e.localizedMessage ?: "Try again bestie!"}"
                }
            }
            Toast.makeText(context, friendlyMsg, Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Sign-In tripped: ${e.localizedMessage ?: "Try email login bestie!"}", Toast.LENGTH_LONG).show()
        }
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isSignUpMode by remember { mutableStateOf(true) }

    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            val userProfile = (authState as AuthState.Success).user
            if (!userProfile.isEmailVerified) {
                onVerificationWaiting()
            } else {
                onAuthSuccess()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAEE)) // Warm ivory background
    ) {
        // Infinite rotation animations for background shapes
        val infiniteTransition = rememberInfiniteTransition(label = "bg_shapes_rotation")
        
        // Gentle smooth continuous rotation for top-right shape (clockwise)
        val topRotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 26000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "top_shape_rot"
        )

        // Gentle smooth continuous rotation for bottom-left shape (counter-clockwise)
        val bottomRotation by infiniteTransition.animateFloat(
            initialValue = 360f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 32000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "bottom_shape_rot"
        )

        // Top-right rotating organic 8-pointed star / flower shape (non-circular)
        Canvas(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.TopEnd)
                .offset(x = 100.dp, y = (-50).dp)
                .rotate(topRotation)
        ) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val path = Path().apply {
                // Smooth organic morphing petal shape
                val rMax = w * 0.48f
                val rMin = w * 0.36f
                val points = 7
                for (i in 0 until points * 2) {
                    val angle = (i * Math.PI / points).toFloat()
                    val radius = if (i % 2 == 0) rMax else rMin
                    val x = cx + radius * kotlin.math.cos(angle)
                    val y = cy + radius * kotlin.math.sin(angle)
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(
                path = path,
                color = Color(0xFFF7ECBC).copy(alpha = 0.55f)
            )
        }

        // Bottom-left rotating soft rounded organic blob polygon (non-circular)
        Canvas(
            modifier = Modifier
                .size(380.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-110).dp, y = 130.dp)
                .rotate(bottomRotation)
        ) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val path = Path().apply {
                // Smooth 5-wave flowing organic pebble shape
                val rMax = w * 0.47f
                val rMin = w * 0.35f
                val points = 5
                for (i in 0 until points * 2) {
                    val angle = (i * Math.PI / points).toFloat()
                    val radius = if (i % 2 == 0) rMax else rMin
                    val x = cx + radius * kotlin.math.cos(angle)
                    val y = cy + radius * kotlin.math.sin(angle)
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(
                path = path,
                color = Color(0xFFF7ECBC).copy(alpha = 0.50f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. BRAND HEADER ROW: Only "FunkyTalk." (Removed "Learn • Share • Connect")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = Color(0xFF141414), fontWeight = FontWeight.Black)) {
                            append("Funky")
                        }
                        withStyle(style = SpanStyle(color = Color(0xFFF6C343), fontWeight = FontWeight.Black)) {
                            append("Talk.")
                        }
                    },
                    fontSize = 28.sp,
                    letterSpacing = (-0.8).sp
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 2. WELCOME TO FUNKYTALK & TITLE
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "WELCOME TO FUNKYTALK",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8E8E93),
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isSignUpMode) "Create your\naccount" else "Welcome\nback",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF141414),
                    lineHeight = 44.sp,
                    letterSpacing = (-1.5).sp,
                    modifier = Modifier.testTag("auth_welcome_title")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isSignUpMode) 
                        "Start connecting with amazing\npeople around the world."
                    else 
                        "Log in to continue chatting with\nyour friends around the world.",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF6B6B70),
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Yellow horizontal accent bar (signature FunkyTalk underline indicator)
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFF6C343))
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 3. INPUT FIELDS
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Email field - Soft pill shape matching reference image
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF141414), fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    placeholder = { Text("Enter your email", color = Color(0xFF9E9E9E), fontSize = 15.sp) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFFF0F0F2), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email Icon",
                                tint = Color(0xFF757575),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("email_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF141414),
                        unfocusedTextColor = Color(0xFF141414),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFFE2E2E6),
                        unfocusedBorderColor = Color(0xFFE5E5EA),
                        cursorColor = Color(0xFFF6C343)
                    ),
                    shape = RoundedCornerShape(22.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                // Password field - Soft pill shape matching reference image
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    textStyle = LocalTextStyle.current.copy(color = Color(0xFF141414), fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    placeholder = { Text(if (isSignUpMode) "Create a password" else "Enter your password", color = Color(0xFF9E9E9E), fontSize = 15.sp) },
                    leadingIcon = {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFFF0F0F2), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Icon",
                                tint = Color(0xFF757575),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isPasswordVisible) "Hide Password" else "Show Password",
                                tint = Color(0xFF757575),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("password_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF141414),
                        unfocusedTextColor = Color(0xFF141414),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFFE2E2E6),
                        unfocusedBorderColor = Color(0xFFE5E5EA),
                        cursorColor = Color(0xFFF6C343)
                    ),
                    shape = RoundedCornerShape(22.dp),
                    singleLine = true,
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
                )

                // Password Strength Meter (Weak / Medium / Strong) for Signup Mode
                val passwordStrength = remember(password) {
                    when {
                        password.isEmpty() -> 0
                        password.length < 6 -> 1 // Weak
                        password.length >= 8 && password.any { it.isDigit() } && password.any { it.isUpperCase() || !it.isLetterOrDigit() } -> 3 // Strong
                        password.length >= 6 -> 2 // Medium
                        else -> 1
                    }
                }

                if (isSignUpMode && password.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        when {
                                            passwordStrength >= 1 -> if (passwordStrength == 1) Color(0xFFEF4444) else if (passwordStrength == 2) Color(0xFFF59E0B) else Color(0xFF10B981)
                                            else -> Color(0xFFE5E7EB)
                                        }
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        when {
                                            passwordStrength >= 2 -> if (passwordStrength == 2) Color(0xFFF59E0B) else Color(0xFF10B981)
                                            else -> Color(0xFFE5E7EB)
                                        }
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (passwordStrength >= 3) Color(0xFF10B981) else Color(0xFFE5E7EB)
                                    )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = when (passwordStrength) {
                                    1 -> "Weak (add numbers/uppercase)"
                                    2 -> "Moderate"
                                    3 -> "Strong password"
                                    else -> ""
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when (passwordStrength) {
                                    1 -> Color(0xFFEF4444)
                                    2 -> Color(0xFFD97706)
                                    3 -> Color(0xFF059669)
                                    else -> Color.Gray
                                }
                            )
                            Text(
                                text = "Min 6 characters",
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }
                }

                // Password hint or Forgot password
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isSignUpMode) {
                        Text(
                            text = "Enter your registered password",
                            fontSize = 12.sp,
                            color = Color(0xFF8E8E93)
                        )
                    }

                    if (!isSignUpMode) {
                        Text(
                            text = "Forgot password?",
                            color = Color(0xFFF6C343),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    if (email.isBlank()) {
                                        Toast.makeText(context, "Drop your email address first, bestie!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.sendPasswordResetEmail(email, context)
                                    }
                                }
                                .testTag("forgot_password_button")
                        )
                    }
                }

                // Error alert states
                if (authState is AuthState.Error) {
                    Text(
                        text = (authState as AuthState.Error).message,
                        color = Color(0xFFD32F2F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 4. CTA BUTTON: "Continue ->" in warm yellow pill
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (email.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Drop your email and password first, bestie!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (password.length < 6) {
                            Toast.makeText(context, "Password's gotta be at least 6 characters, bestie!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (isSignUpMode) {
                            viewModel.signUpWithEmail(
                                email = email,
                                password = password,
                                context = context,
                                onLoginNeeded = onAuthSuccess,
                                onVerificationSent = onVerificationWaiting
                            )
                        } else {
                            viewModel.loginWithEmail(
                                email = email,
                                password = password,
                                context = context,
                                onSignUpNeeded = onVerificationWaiting,
                                onSuccess = onAuthSuccess
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("submit_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF7C343),
                        contentColor = Color(0xFF141414)
                    ),
                    shape = RoundedCornerShape(30.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(
                            color = Color(0xFF141414),
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Continue",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF141414)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Arrow",
                                tint = Color(0xFF141414),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 5. OR DIVIDER
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFDCDCE0)
                )
                Text(
                    text = "OR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFDCDCE0)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. CONTINUE WITH GOOGLE BUTTON
            Button(
                onClick = {
                    if (isRealGoogleAuthAvailable) {
                        try {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(webClientId)
                                .requestEmail()
                                .build()
                            val googleSignInClient = GoogleSignIn.getClient(context, gso)
                            googleSignInClient.signOut().addOnCompleteListener {
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Google Sign-In hit a bump: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Google Sign-In is still setting up! Hop in with Email login for now, bestie.", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .border(1.dp, Color(0xFFE2E2E8), RoundedCornerShape(29.dp))
                    .testTag("google_login_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF141414)
                ),
                shape = RoundedCornerShape(29.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google Logo",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF141414)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 7. TOGGLE: Already have an account? Log In
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isSignUpMode) "Already have an account? " else "Don't have an account? ",
                    fontSize = 15.sp,
                    color = Color(0xFF6B6B70)
                )
                Text(
                    text = if (isSignUpMode) "Log In" else "Sign Up",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF7C343),
                    modifier = Modifier
                        .clickable { isSignUpMode = !isSignUpMode }
                        .testTag("toggle_auth_mode")
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

