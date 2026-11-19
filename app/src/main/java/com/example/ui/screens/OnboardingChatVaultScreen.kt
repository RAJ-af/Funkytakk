package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.crypto.E2EEManager
import com.example.crypto.VaultSecurityManager
import com.example.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun OnboardingChatVaultScreen(
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    SecureWindow(enabled = true)
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var enableBiometrics by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val canAuthenticateBiometric = remember {
        val bm = BiometricManager.from(context)
        bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    BackHandler { onBack() }

    Scaffold(
        containerColor = Color(0xFFFAFAEE),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        SoftBackIcon(
                            size = 22.dp,
                            color = Color(0xFF1F2937)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Security Setup",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Soft Badge with Custom Soft Lock Icon
            SoftIconBadge(
                badgeSize = 72.dp,
                backgroundColor = Color(0xFFFEF3C7),
                borderColor = Color(0xFFFDE68A)
            ) {
                SoftLockIcon(
                    size = 36.dp,
                    isUnlocked = false,
                    gradientColors = listOf(Color(0xFFFBBF24), Color(0xFFD97706)),
                    shackleColor = Color(0xFF92400E)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Secure Your 1:1 Chats",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1F2937),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "FunkyTalk uses Google Tink (HPKE RFC 9180) end-to-end encryption. Your local chat history is locked with SQLCipher envelope encryption.",
                fontSize = 13.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Crucial Local Storage Notice Box
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFFBEB),
                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SoftWarningIcon(
                            size = 20.dp,
                            color = Color(0xFFD97706)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Chat history is local to this device",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Because the server is a temporary mailbox that deletes messages immediately upon delivery, your encrypted chats live only on this phone. If the app is uninstalled or device is reset, chat history is unrecoverable by design.",
                        fontSize = 11.sp,
                        color = Color(0xFFB45309),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text("Vault Password (min 4 characters)") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        SoftEyeIcon(
                            visible = passwordVisible,
                            size = 20.dp,
                            tint = if (passwordVisible) Color(0xFFB45309) else Color(0xFF9CA3AF)
                        )
                    }
                },
                textStyle = TextStyle(
                    color = Color(0xFF111827),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF111827),
                    unfocusedTextColor = Color(0xFF111827),
                    focusedBorderColor = Color(0xFFFFC529),
                    focusedLabelColor = Color(0xFFB45309),
                    cursorColor = Color(0xFF111827),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    errorMessage = null
                },
                label = { Text("Confirm Vault Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        SoftEyeIcon(
                            visible = passwordVisible,
                            size = 20.dp,
                            tint = if (passwordVisible) Color(0xFFB45309) else Color(0xFF9CA3AF)
                        )
                    }
                },
                textStyle = TextStyle(
                    color = Color(0xFF111827),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF111827),
                    unfocusedTextColor = Color(0xFF111827),
                    focusedBorderColor = Color(0xFFFFC529),
                    focusedLabelColor = Color(0xFFB45309),
                    cursorColor = Color(0xFF111827),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (canAuthenticateBiometric) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = enableBiometrics,
                        onCheckedChange = { enableBiometrics = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Enable Biometric unlock (Fingerprint / Face)",
                        fontSize = 13.sp,
                        color = Color(0xFF374151)
                    )
                }
            }

            errorMessage?.let { err ->
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = err,
                    fontSize = 12.sp,
                    color = Color(0xFFDC2626),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (password.length < 4) {
                        errorMessage = "Password must be at least 4 characters."
                        return@Button
                    }
                    if (password != confirmPassword) {
                        errorMessage = "Passwords do not match."
                        return@Button
                    }
                    isLoading = true
                    scope.launch {
                        val setupRes = VaultSecurityManager.setupVaultWithPassword(context, password)
                        setupRes.onSuccess {
                            // Cache vault password for background backups
                            com.example.util.ChatBackupPreferencesManager.setCachedVaultPassword(context, password)

                            // Ensure Tink identity key initialized
                            E2EEManager.ensureIdentityKeyInitialized(context)

                            if (enableBiometrics && canAuthenticateBiometric && activity != null) {
                                val cryptoResult = VaultSecurityManager.createBiometricEncryptCryptoObject()
                                cryptoResult.onSuccess { cryptoObject ->
                                    val executor = ContextCompat.getMainExecutor(activity)
                                    val prompt = BiometricPrompt(
                                        activity,
                                        executor,
                                        object : BiometricPrompt.AuthenticationCallback() {
                                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                                super.onAuthenticationSucceeded(result)
                                                result.cryptoObject?.cipher?.let { cipher ->
                                                    VaultSecurityManager.saveBiometricWrappedVaultKey(activity, cipher)
                                                }
                                                isLoading = false
                                                Toast.makeText(context, "Chat Vault initialized!", Toast.LENGTH_SHORT).show()
                                                onComplete()
                                            }

                                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                                super.onAuthenticationError(errorCode, errString)
                                                isLoading = false
                                                onComplete()
                                            }
                                        }
                                    )
                                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                                        .setTitle("Enable Biometric Unlock")
                                        .setSubtitle("Confirm biometrics to unlock chat vault")
                                        .setNegativeButtonText("Skip")
                                        .build()
                                    try {
                                        prompt.authenticate(promptInfo, cryptoObject)
                                    } catch (e: Exception) {
                                        isLoading = false
                                        onComplete()
                                    }
                                }.onFailure {
                                    isLoading = false
                                    onComplete()
                                }
                            } else {
                                isLoading = false
                                Toast.makeText(context, "Chat Vault initialized!", Toast.LENGTH_SHORT).show()
                                onComplete()
                            }
                        }.onFailure { ex ->
                            isLoading = false
                            errorMessage = ex.message ?: "Failed to setup vault"
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                } else {
                    Text("Set Up Vault & Continue", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            TextButton(
                onClick = {
                    scope.launch {
                        E2EEManager.ensureIdentityKeyInitialized(context)
                    }
                    onComplete()
                }
            ) {
                Text(
                    text = "Skip for now (Set up later in Chats)",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
    }
}
