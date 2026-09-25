package com.example.ui.screens.chat

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.crypto.VaultSecurityManager
import com.example.crypto.VaultStatus
import com.example.repository.EncryptedChatRepository
import kotlinx.coroutines.launch

@Composable
fun VaultManagementDialogs(
    onDismissRequest: () -> Unit = {}
) {
    val context = LocalContext.current
    val vaultStatus by VaultSecurityManager.vaultStatus.collectAsState()

    when (vaultStatus) {
        VaultStatus.NOT_CONFIGURED -> {
            VaultSetupDialog(onDismiss = onDismissRequest)
        }
        VaultStatus.LOCKED -> {
            VaultUnlockDialog(onDismiss = onDismissRequest)
        }
        VaultStatus.UNLOCKED -> {
            // Already unlocked
        }
    }
}

@Composable
fun VaultSetupDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFFEF3C7), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Encrypted Vault",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Setup Encrypted Chat Vault",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "FunkyTalk 1:1 messages use Google Tink (HPKE RFC 9180) end-to-end encryption. Your local chat history is locked with SQLCipher.",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Critical Device Storage Notice Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Notice",
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Chat history is stored locally on this device only. The server never holds plaintext and deletes delivered messages immediately. If reinstalled or reset, chats cannot be recovered.",
                            fontSize = 11.sp,
                            color = Color(0xFF92400E),
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Password Inputs
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Vault Password (min 4 chars)") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = null
                    },
                    label = { Text("Confirm Vault Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (canAuthenticateBiometric) {
                    Spacer(modifier = Modifier.height(8.dp))
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
                            text = "Enable Biometric unlock (Fingerprint/Face)",
                            fontSize = 12.sp,
                            color = Color(0xFF374151)
                        )
                    }
                }

                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = err,
                        fontSize = 12.sp,
                        color = Color(0xFFDC2626),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                            val setupResult = VaultSecurityManager.setupVaultWithPassword(context, password)
                            setupResult.onSuccess { vaultKey ->
                                if (enableBiometrics && canAuthenticateBiometric && activity != null) {
                                    val cryptoResult = VaultSecurityManager.createBiometricEncryptCryptoObject()
                                    cryptoResult.onSuccess { cryptoObject ->
                                        promptBiometricSetup(activity, cryptoObject) { success ->
                                            isLoading = false
                                            Toast.makeText(context, "Encrypted Chat Vault initialized!", Toast.LENGTH_SHORT).show()
                                        }
                                    }.onFailure {
                                        isLoading = false
                                        Toast.makeText(context, "Encrypted Chat Vault initialized with password.", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "Encrypted Chat Vault initialized!", Toast.LENGTH_SHORT).show()
                                }
                            }.onFailure { ex ->
                                isLoading = false
                                errorMessage = ex.message ?: "Failed to initialize vault"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937)),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("Create & Lock Vault", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun VaultUnlockDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()

    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }

    val hasBiometric = remember { VaultSecurityManager.isBiometricEnabled(context) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFEFF6FF), RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Unlock Vault",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Unlock Chat Vault",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter your vault password or authenticate to unwrap your encryption keys and decrypt your conversations.",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Vault Password") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        if (password.isNotBlank() && !isLoading) {
                            isLoading = true
                            scope.launch {
                                val res = VaultSecurityManager.unlockWithPassword(context, password)
                                isLoading = false
                                res.onFailure { ex ->
                                    errorMessage = ex.message ?: "Incorrect password"
                                }
                            }
                        }
                    }),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = err,
                        fontSize = 12.sp,
                        color = Color(0xFFDC2626),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Unlock Button
                Button(
                    onClick = {
                        if (password.isBlank()) {
                            errorMessage = "Please enter your vault password."
                            return@Button
                        }
                        isLoading = true
                        scope.launch {
                            val res = VaultSecurityManager.unlockWithPassword(context, password)
                            isLoading = false
                            res.onFailure { ex ->
                                errorMessage = ex.message ?: "Incorrect password"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text("Unlock with Password", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Biometrics option if enabled
                if (hasBiometric && activity != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            val cryptoObjResult = VaultSecurityManager.createBiometricDecryptCryptoObject(context)
                            cryptoObjResult.onSuccess { cryptoObj ->
                                promptBiometricUnlock(activity, cryptoObj) { success ->
                                    if (!success) {
                                        errorMessage = "Biometric authentication cancelled or failed."
                                    }
                                }
                            }.onFailure { ex ->
                                errorMessage = ex.message ?: "Biometric unlock unavailable."
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFD1D5DB))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometrics",
                            tint = Color(0xFF374151),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unlock with Biometrics", color = Color(0xFF374151), fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Reset Vault option
                TextButton(onClick = { showResetConfirmation = true }) {
                    Text(
                        text = "Forgot password? Reset Vault",
                        fontSize = 12.sp,
                        color = Color(0xFFDC2626)
                    )
                }
            }
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Reset Warning",
                    tint = Color(0xFFDC2626)
                )
            },
            title = { Text("Reset Encrypted Vault?") },
            text = {
                Text(
                    "Resetting the vault permanently deletes the local encryption key and wipes all 1:1 chat history stored on this device. Messages cannot be restored from the server. Are you sure you want to proceed?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirmation = false
                        VaultSecurityManager.resetVault(context)
                        Toast.makeText(context, "Vault reset. Please set up a new password.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Reset & Erase History", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun promptBiometricSetup(
    activity: FragmentActivity,
    cryptoObject: BiometricPrompt.CryptoObject,
    onComplete: (Boolean) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Enable Biometric Unlock")
        .setSubtitle("Confirm your fingerprint or face to protect your E2EE chat vault")
        .setNegativeButtonText("Skip for now")
        .build()

    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                val cipher = result.cryptoObject?.cipher
                if (cipher != null) {
                    VaultSecurityManager.saveBiometricWrappedVaultKey(activity, cipher)
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onComplete(false)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Handled in prompt UI
            }
        }
    )

    try {
        prompt.authenticate(promptInfo, cryptoObject)
    } catch (e: Exception) {
        onComplete(false)
    }
}

private fun promptBiometricUnlock(
    activity: FragmentActivity,
    cryptoObject: BiometricPrompt.CryptoObject,
    onComplete: (Boolean) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock Chat Vault")
        .setSubtitle("Authenticate using your fingerprint or face")
        .setNegativeButtonText("Use Password")
        .build()

    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                val cipher = result.cryptoObject?.cipher
                if (cipher != null) {
                    val res = VaultSecurityManager.unlockWithBiometricCipher(activity, cipher)
                    onComplete(res.isSuccess)
                } else {
                    onComplete(false)
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onComplete(false)
            }
        }
    )

    try {
        prompt.authenticate(promptInfo, cryptoObject)
    } catch (e: Exception) {
        onComplete(false)
    }
}
