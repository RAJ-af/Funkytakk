package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.Icon
import com.example.AuthState
import com.example.AuthViewModel
import com.example.R
import com.example.BuildConfig
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

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
            val idToken = account.idToken!!
            viewModel.loginWithGoogle(context, idToken, onAuthSuccess)
        } catch (e: Exception) {
            Toast.makeText(context, "Google Sign-In failed: " + (e.localizedMessage ?: "Unknown error"), Toast.LENGTH_LONG).show()
        }
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isSignUpMode by remember { mutableStateOf(true) }
    var showGoogleAccountChooser by remember { mutableStateOf(false) }

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
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF7E8), // Tender golden warm cream
                        Color(0xFFFBF1D5), // Soft pastel sunset bronze
                        Color(0xFFEAF2F9)  // Soft premium azure blue haze
                    )
                )
            )
    ) {
        // Star decorative sparkle crosses matching Screenshot 3
        DecorativeBackgroundAccents()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            // Screen Header Panel
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Welcome to",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black.copy(alpha = 0.5f),
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "FunkyTalk",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    letterSpacing = (-1.5).sp,
                    lineHeight = 48.sp,
                    modifier = Modifier.testTag("auth_welcome_title")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Meet language partners\nfrom around the world! 🌍💬✨",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black.copy(alpha = 0.6f),
                    lineHeight = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Fields Input panel
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    textStyle = LocalTextStyle.current.copy(color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium),
                    placeholder = { Text("Enter your email", color = Color.Black.copy(alpha = 0.4f), fontSize = 16.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email Icon",
                            tint = Color.Black.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("email_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFFFFC529),
                        unfocusedBorderColor = Color.Black.copy(alpha = 0.12f),
                        cursorColor = Color(0xFFFFC529)
                    ),
                    shape = CircleShape,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    textStyle = LocalTextStyle.current.copy(color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Medium),
                    placeholder = { Text(if (isSignUpMode) "Create a password" else "Enter your password", color = Color.Black.copy(alpha = 0.4f), fontSize = 16.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Icon",
                            tint = Color.Black.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isPasswordVisible) "Hide Password" else "Show Password",
                                tint = Color.Black.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("password_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFFFFC529),
                        unfocusedBorderColor = Color.Black.copy(alpha = 0.12f),
                        cursorColor = Color(0xFFFFC529)
                    ),
                    shape = CircleShape,
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

                // Password field helper text or guidelines
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Password must be at least 6 characters.",
                        color = Color.Black.copy(alpha = 0.45f),
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (!isSignUpMode) {
                        Text(
                            text = "Forgot Password?",
                            color = Color(0xFFFF9E00),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    if (email.isBlank()) {
                                        Toast.makeText(context, "Please enter your email address in the field above first.", Toast.LENGTH_LONG).show()
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
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                // Beautiful Adaptive Guide notice
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFFC529).copy(alpha = 0.35f))
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡",
                            fontSize = 18.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = if (isSignUpMode) {
                                "If you already have an account, entering your password will automatically sign you in safely!"
                            } else {
                                "If you don't have an account yet, entering an email and password will securely create one for you automatically!"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                             color = Color.Black.copy(alpha = 0.7f),
                            lineHeight = 16.sp
                        )
                    }
                }

                // CTA Button: Beautiful Gold Pill With Arrow
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (email.isBlank() || password.isBlank()) {
                            Toast.makeText(context, "Please enter both email and password.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (password.length < 6) {
                            Toast.makeText(context, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show()
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
                        .height(58.dp)
                        .testTag("submit_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC529),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(29.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isSignUpMode) "Get Started" else "Log In",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Arrow",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Toggle Row Sign Up vs Log In
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSignUpMode) "Already have an account? " else "Don't have an account? ",
                        fontSize = 14.sp,
                        color = Color.Black.copy(alpha = 0.55f)
                    )
                    Text(
                        text = if (isSignUpMode) "Log In" else "Sign Up",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9E00),
                        modifier = Modifier
                            .clickable { isSignUpMode = !isSignUpMode }
                            .testTag("toggle_auth_mode")
                    )
                }
            }

            // Divider OR indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.Black.copy(alpha = 0.08f)
                )
                Text(
                    text = "OR",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black.copy(alpha = 0.35f),
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.Black.copy(alpha = 0.08f)
                )
            }

            // Google login pill button (Capsule border)
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
                            Toast.makeText(context, "Google sign in error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        showGoogleAccountChooser = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, Color.Black.copy(alpha = 0.12f), CircleShape)
                    .testTag("google_login_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = CircleShape,
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
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showGoogleAccountChooser) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showGoogleAccountChooser = false }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Google logo
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_google),
                            contentDescription = "Google Logo",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Choose an account",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "to continue to FunkyTalk",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Account 1: Shwetabh shwetabhatingar@gmail.com
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showGoogleAccountChooser = false
                                viewModel.simulateGoogleLogin(
                                    context, 
                                    "shwetabhatingar@gmail.com", 
                                    "Shwetabh", 
                                    "google-uid-72", 
                                    onAuthSuccess
                                )
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFC529)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "S",
                                color = Color.Black,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Shwetabh",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "shwetabhatingar@gmail.com",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    HorizontalDivider(color = Color.Black.copy(alpha = 0.08f))

                    // Account 2: FunkyTalk Test User funkytest@gmail.com
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showGoogleAccountChooser = false
                                viewModel.simulateGoogleLogin(
                                    context, 
                                    "funkytest@gmail.com", 
                                    "FunkyTalk Test User", 
                                    "google-uid-test-100", 
                                    onAuthSuccess
                                )
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEAF2F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "F",
                                color = Color(0xFF4285F4),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FunkyTalk Test User",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "funkytest@gmail.com",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    HorizontalDivider(color = Color.Black.copy(alpha = 0.08f))

                    // Account 3: Add new test account / Setup from Scratch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showGoogleAccountChooser = false
                                viewModel.simulateGoogleLogin(
                                    context, 
                                    "new_learner_99@gmail.com", 
                                    "New Learner", 
                                    "google-uid-new-" + System.currentTimeMillis().toString().takeLast(6), 
                                    onAuthSuccess
                                )
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF3F4F6)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Add another account",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Text(
                                text = "Sign in to a different profile from scratch",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = { showGoogleAccountChooser = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF9E00))
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DecorativeBackgroundAccents() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Star cross accent 1
        Text(
            text = "+",
            fontSize = 24.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF4285F4).copy(alpha = 0.2f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 40.dp, y = 110.dp)
        )

        // Star cross accent 2
        Text(
            text = "+",
            fontSize = 32.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFFFFB300).copy(alpha = 0.2f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-40).dp, y = 170.dp)
        )

        // Star cross accent 3
        Text(
            text = "+",
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF4285F4).copy(alpha = 0.15f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (-30).dp, y = 140.dp)
        )
    }
}
