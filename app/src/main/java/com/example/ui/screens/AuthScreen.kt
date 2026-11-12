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
import androidx.compose.material.icons.filled.Warning
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
import com.example.GoogleServicesConfig
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import android.content.pm.PackageManager
import java.security.MessageDigest

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    onVerificationWaiting: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val webClientId = GoogleServicesConfig.getWebClientId(context)
    val isRealGoogleAuthAvailable = webClientId.isNotEmpty()

    var showGoogleDiagnosticDialog by remember { mutableStateOf(false) }
    var detectedSha1 by remember { mutableStateOf("") }
    var detectedPackage by remember { mutableStateOf(context.packageName) }

    fun extractRuntimeSha1(): String {
        return try {
            val pm = context.packageManager
            val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            val packageInfo = pm.getPackageInfo(context.packageName, flags)
            val certs = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            if (!certs.isNullOrEmpty()) {
                val md = MessageDigest.getInstance("SHA-1")
                val digest = md.digest(certs[0].toByteArray())
                digest.joinToString(":") { String.format("%02X", it) }
            } else {
                "Not available"
            }
        } catch (e: Exception) {
            "Error extracting SHA-1: ${e.localizedMessage}"
        }
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isSignUpMode by remember { mutableStateOf(true) }

    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            val idToken = account.idToken
            if (idToken != null) {
                viewModel.loginWithGoogle(
                    context = context,
                    idToken = idToken,
                    onSuccess = onAuthSuccess
                )
            } else {
                Toast.makeText(context, "Google didn't share your auth token, bestie! Try again.", Toast.LENGTH_LONG).show()
            }
        } catch (e: ApiException) {
            if (e.statusCode == CommonStatusCodes.DEVELOPER_ERROR) {
                detectedSha1 = extractRuntimeSha1()
                showGoogleDiagnosticDialog = true
            }
            val friendlyMsg = when (e.statusCode) {
                CommonStatusCodes.DEVELOPER_ERROR -> { // Code 10
                    "Google Developer Error (Code 10): SHA-1 mismatch! Tap info on screen."
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

    val authState by viewModel.authState.collectAsState()
    val isLoading = authState is AuthState.Loading

    // Email format validator regex
    val isEmailFormatValid: (String) -> Boolean = { input ->
        android.util.Patterns.EMAIL_ADDRESS.matcher(input.trim()).matches()
    }

    // Function to submit authentication
    val submitAuth: () -> Unit = {
        focusManager.clearFocus()
        val trimmedEmail = email.trim()
        email = trimmedEmail // Apply trimming

        var hasError = false

        if (trimmedEmail.isEmpty()) {
            emailError = "Please enter your email"
            hasError = true
        } else if (!isEmailFormatValid(trimmedEmail)) {
            emailError = "Please enter a valid email format (e.g. user@example.com)"
            hasError = true
        } else {
            emailError = null
        }

        if (password.isEmpty()) {
            passwordError = "Please enter your password"
            hasError = true
        } else if (isSignUpMode && password.length < 6) {
            passwordError = "Password must be at least 6 characters"
            hasError = true
        } else {
            passwordError = null
        }

        if (!hasError && !isLoading) {
            if (isSignUpMode) {
                viewModel.signUpWithEmail(
                    email = trimmedEmail,
                    password = password,
                    context = context,
                    onLoginNeeded = onAuthSuccess,
                    onVerificationSent = onVerificationWaiting
                )
            } else {
                viewModel.loginWithEmail(
                    email = trimmedEmail,
                    password = password,
                    context = context,
                    onSignUpNeeded = onVerificationWaiting,
                    onSuccess = onAuthSuccess
                )
            }
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Loading) {
            com.example.ui.components.AppLoadingManager.show(if (isSignUpMode) "Creating your account..." else "Logging in...")
        } else {
            com.example.ui.components.AppLoadingManager.hide()
        }
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Email field - Soft pill shape matching reference image
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { 
                            email = it 
                            emailError = null
                        },
                        enabled = !isLoading,
                        isError = emailError != null,
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
                            disabledContainerColor = Color(0xFFF3F4F6),
                            focusedBorderColor = Color(0xFFE2E2E6),
                            unfocusedBorderColor = Color(0xFFE5E5EA),
                            errorBorderColor = Color(0xFFEF4444),
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

                    // Email error text
                    if (emailError != null) {
                        Text(
                            text = emailError!!,
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                        )
                    }
                }

                // Password field - Soft pill shape matching reference image
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { 
                            password = it 
                            passwordError = null
                        },
                        enabled = !isLoading,
                        isError = passwordError != null,
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
                            disabledContainerColor = Color(0xFFF3F4F6),
                            focusedBorderColor = Color(0xFFE2E2E6),
                            unfocusedBorderColor = Color(0xFFE5E5EA),
                            errorBorderColor = Color(0xFFEF4444),
                            cursorColor = Color(0xFFF6C343)
                        ),
                        shape = RoundedCornerShape(22.dp),
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (isSignUpMode) ImeAction.Done else ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { submitAuth() },
                            onGo = { submitAuth() }
                        )
                    )

                    // Password error text
                    if (passwordError != null) {
                        Text(
                            text = passwordError!!,
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 12.dp, top = 4.dp)
                        )
                    }
                }

                // Password Strength Meter (Only for Sign Up Mode)
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

                // Forgot password (Clean & Right-aligned in Log In mode)
                if (!isSignUpMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Forgot password?",
                            color = Color(0xFFF6C343),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable(enabled = !isLoading) {
                                    val trimmed = email.trim()
                                    if (trimmed.isBlank()) {
                                        Toast.makeText(context, "Please enter your email address first.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.sendPasswordResetEmail(trimmed, context)
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
                } else if (authState is AuthState.AccountCollision) {
                    Text(
                        text = (authState as AuthState.AccountCollision).message,
                        color = Color(0xFFD32F2F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 4. CTA BUTTON: "Continue ->" in warm yellow pill
                Button(
                    onClick = { submitAuth() },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("submit_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF7C343),
                        contentColor = Color(0xFF141414),
                        disabledContainerColor = Color(0xFFE5E7EB),
                        disabledContentColor = Color(0xFF9CA3AF)
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

        if (showGoogleDiagnosticDialog) {
            AlertDialog(
                onDismissRequest = { showGoogleDiagnosticDialog = false },
                title = {
                    Text(
                        text = "Google Sign-In SHA-1 Setup",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF141414)
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Firebase needs the exact SHA-1 fingerprint of this installed APK. Here is the SHA-1 detected on your device right now:",
                            fontSize = 14.sp,
                            color = Color(0xFF49454F)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF4F4F6),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = detectedSha1,
                                fontSize = 12.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF141414),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Package: $detectedPackage",
                            fontSize = 12.sp,
                            color = Color(0xFF6B6B70)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Web Client ID: $webClientId",
                            fontSize = 11.sp,
                            color = Color(0xFF6B6B70)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("SHA-1 Fingerprint", detectedSha1)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "SHA-1 copied to clipboard! Paste into Firebase Console.", Toast.LENGTH_SHORT).show()
                            showGoogleDiagnosticDialog = false
                        }
                    ) {
                        Text("Copy SHA-1")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGoogleDiagnosticDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }

        // Account Collision Dialog (Email Already Registered with Google or Email/Password)
        if (authState is AuthState.AccountCollision) {
            val collision = authState as AuthState.AccountCollision
            AlertDialog(
                onDismissRequest = {
                    viewModel.resetAuthState()
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFFFFF7DB), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Account Exists",
                            tint = Color(0xFFF7C343),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Account Already Exists",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF141414),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Only 1 account is permitted per email address.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF141414),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF4F4F6)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email",
                                    tint = Color(0xFF141414),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = collision.email.ifBlank { "Registered Email" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF141414),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "An account with this email already exists. Please select Log In to enter your password or sign in with Google.",
                            fontSize = 13.sp,
                            color = Color(0xFF6B6B70),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isSignUpMode = false // switch tab to Log In mode
                            if (collision.email.isNotBlank()) {
                                email = collision.email
                            }
                            viewModel.resetAuthState()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF7C343),
                            contentColor = Color(0xFF141414)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Go to Log In", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            viewModel.resetAuthState()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Got It", color = Color(0xFF6B6B70), fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    }
}

