package com.example.ui.screens.chat

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
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
import com.example.crypto.VaultCooldownException
import com.example.crypto.VaultSecurityManager
import com.example.crypto.VaultStatus
import com.example.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VaultManagementDialogs(
    onDismissRequest: () -> Unit = {}
) {
    val vaultStatus by VaultSecurityManager.vaultStatus.collectAsState()

    when (vaultStatus) {
        VaultStatus.NOT_CONFIGURED -> {
            VaultSetupDialog(onDismiss = onDismissRequest)
        }
        VaultStatus.LOCKED -> {
            VaultUnlockDialog(onDismiss = onDismissRequest)
        }
        VaultStatus.UNLOCKED -> {
            // Unlocked and ready
        }
    }
}

/**
 * Modern good-looking Setup Password dialog using custom soft icons only.
 */
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
    var confirmPasswordVisible by remember { mutableStateOf(false) }
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
        SecureWindow(enabled = true)
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color.White,
            shadowElevation = 20.dp,
            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Soft Badge with Custom Soft Lock Icon
                SoftIconBadge(
                    badgeSize = 64.dp,
                    backgroundColor = Color(0xFFFFFBEB),
                    borderColor = Color(0xFFFDE68A)
                ) {
                    SoftLockIcon(
                        size = 32.dp,
                        isUnlocked = false,
                        gradientColors = listOf(Color(0xFFFBBF24), Color(0xFFD97706)),
                        shackleColor = Color(0xFF92400E)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Set Up Chat Lock",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF111827),
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Create a password to lock and encrypt your private conversations with end-to-end security.",
                    fontSize = 13.sp,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Sleek, minimal notice badge using Custom Soft Shield Icon
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SoftShieldIcon(
                            size = 18.dp,
                            gradientColors = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                            showCheck = true
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Stored securely on this device. Never uploaded to servers.",
                            fontSize = 11.sp,
                            color = Color(0xFF92400E),
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Password Input
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Chat Password (min 4 chars)") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { passwordVisible = !passwordVisible },
                            contentAlignment = Alignment.Center
                        ) {
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Confirm Password Input
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        errorMessage = null
                    },
                    label = { Text("Confirm Chat Password") },
                    singleLine = true,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { confirmPasswordVisible = !confirmPasswordVisible },
                            contentAlignment = Alignment.Center
                        ) {
                            SoftEyeIcon(
                                visible = confirmPasswordVisible,
                                size = 20.dp,
                                tint = if (confirmPasswordVisible) Color(0xFFB45309) else Color(0xFF9CA3AF)
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                if (canAuthenticateBiometric) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF9FAFB),
                        border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { enableBiometrics = !enableBiometrics }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SoftFingerprintIcon(
                                size = 22.dp,
                                color = if (enableBiometrics) Color(0xFF2563EB) else Color(0xFF9CA3AF)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Enable Biometric unlock",
                                fontSize = 13.sp,
                                color = Color(0xFF374151),
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = enableBiometrics,
                                onCheckedChange = { enableBiometrics = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF111827)
                                )
                            )
                        }
                    }
                }

                errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SoftWarningIcon(size = 18.dp, color = Color(0xFFDC2626))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = err,
                            fontSize = 12.sp,
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Submit Button
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
                            setupResult.onSuccess {
                                com.example.util.ChatBackupPreferencesManager.setCachedVaultPassword(context, password)
                                if (enableBiometrics && canAuthenticateBiometric && activity != null) {
                                    val cryptoResult = VaultSecurityManager.createBiometricEncryptCryptoObject()
                                    cryptoResult.onSuccess { cryptoObject ->
                                        promptBiometricSetup(activity, cryptoObject) {
                                            isLoading = false
                                            Toast.makeText(context, "Chat lock active! Chats unlocked.", Toast.LENGTH_SHORT).show()
                                        }
                                    }.onFailure {
                                        isLoading = false
                                        Toast.makeText(context, "Chat lock active! Chats unlocked.", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    isLoading = false
                                    Toast.makeText(context, "Chat lock active! Chats unlocked.", Toast.LENGTH_SHORT).show()
                                }
                            }.onFailure { ex ->
                                isLoading = false
                                errorMessage = ex.message ?: "Failed to set up chat password"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827)),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = "Set Password & Unlock",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modern good-looking Unlock Password dialog using custom soft icons only.
 */
@Composable
fun VaultUnlockDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val lockUiState by com.example.crypto.ChatLockManager.uiState.collectAsState()
    LaunchedEffect(context) {
        com.example.crypto.ChatLockManager.refreshState(context)
    }

    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showPasswordForm by remember { mutableStateOf(false) }
    var unlockSuccessAnimating by remember { mutableStateOf(false) }
    var cooldownSeconds by remember { mutableIntStateOf(VaultSecurityManager.getRemainingCooldownSeconds(context)) }
    var showForgotOptionsDialog by remember { mutableStateOf(false) }
    var showRestoreBackupOverlay by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }

    val qaVaultPassword = remember { VaultSecurityManager.getQaTestVaultPassword() }
    val requiresPasswordOnly = lockUiState.requirement != com.example.crypto.UnlockRequirement.LEVEL_1_BIOMETRIC_OR_DEVICE_CREDENTIAL
    val isPasswordMode = requiresPasswordOnly || showPasswordForm

    val lockScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (unlockSuccessAnimating) 1.18f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.5f,
            stiffness = 380f
        ),
        label = "vault_lock_scale"
    )

    fun triggerUnlockAnimationAndCommit(keyToCommit: ByteArray?) {
        if (unlockSuccessAnimating) return
        unlockSuccessAnimating = true
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        scope.launch {
            delay(260L)
            if (keyToCommit != null) {
                com.example.crypto.ChatLockManager.finalizePreparedUnlock(context, keyToCommit)
            }
        }
    }

    fun launchLevel1Unlock() {
        if (activity == null || unlockSuccessAnimating) return
        errorMessage = null
        com.example.crypto.ChatLockManager.launchLevel1Prompt(
            activity = activity,
            onSuccess = {
                com.example.crypto.ChatLockManager.prepareLevel1Unlock(context)
                    .onSuccess { key -> triggerUnlockAnimationAndCommit(key) }
                    .onFailure { ex ->
                        errorMessage = ex.message ?: "Please enter your vault password."
                        showPasswordForm = true
                    }
            },
            onUsePasswordFallback = {
                showPasswordForm = true
            },
            onError = { msg ->
                errorMessage = msg
                showPasswordForm = true
            }
        )
    }

    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1000L)
            val next = VaultSecurityManager.getRemainingCooldownSeconds(context)
            cooldownSeconds = next
            if (next == 0 && errorMessage?.contains("Too many", ignoreCase = true) == true) {
                errorMessage = null
            }
        }
    }

    // Auto-launch Level 1 BiometricPrompt when permitted
    LaunchedEffect(lockUiState.requirement) {
        if (lockUiState.requirement == com.example.crypto.UnlockRequirement.LEVEL_1_BIOMETRIC_OR_DEVICE_CREDENTIAL &&
            !showPasswordForm &&
            activity != null
        ) {
            launchLevel1Unlock()
        }
    }

    fun submitPasswordUnlock() {
        if (isLoading || cooldownSeconds > 0 || unlockSuccessAnimating) return
        if (password.isBlank()) {
            errorMessage = "Please enter your vault password."
            return
        }
        isLoading = true
        scope.launch {
            val res = VaultSecurityManager.unlockWithPassword(context, password)
            isLoading = false
            res.onSuccess {
                cooldownSeconds = 0
                com.example.util.ChatBackupPreferencesManager.setCachedVaultPassword(context, password)
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
            }
            res.onFailure { ex ->
                if (ex is VaultCooldownException) {
                    cooldownSeconds = ex.remainingSeconds
                } else {
                    cooldownSeconds = VaultSecurityManager.getRemainingCooldownSeconds(context)
                }
                errorMessage = ex.message ?: "Incorrect password. Please try again."
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        SecureWindow(enabled = true)
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = Color.White,
            shadowElevation = 20.dp,
            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.graphicsLayer(scaleX = lockScale, scaleY = lockScale)) {
                    SoftIconBadge(
                        badgeSize = 64.dp,
                        backgroundColor = if (unlockSuccessAnimating) Color(0xFFDCFCE7) else Color(0xFFEEF2FF),
                        borderColor = if (unlockSuccessAnimating) Color(0xFF86EFAC) else Color(0xFFC7D2FE)
                    ) {
                        SoftLockIcon(
                            size = 32.dp,
                            isUnlocked = unlockSuccessAnimating,
                            gradientColors = if (unlockSuccessAnimating) {
                                listOf(Color(0xFF22C55E), Color(0xFF16A34A))
                            } else {
                                listOf(Color(0xFF6366F1), Color(0xFF4338CA))
                            },
                            shackleColor = Color(0xFFCBD5E1)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when {
                        unlockSuccessAnimating -> "Unlocked"
                        isPasswordMode -> "Enter Vault Password"
                        else -> "Unlock Chats"
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF111827),
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                val subtitleText = when {
                    !lockUiState.hasScreenLockOrBiometric ->
                        "Set a screen lock on your phone for faster unlock."
                    lockUiState.biometricEnrollmentChanged ->
                        "Your device biometrics changed. Please verify your vault password."
                    lockUiState.is14DayPasswordExpired ->
                        "Your vault password is asked again every 14 days."
                    !lockUiState.hasRememberedKey ->
                        "Verify your vault password once to remember this device."
                    isPasswordMode ->
                        "Please enter your vault password to unlock your conversations."
                    else ->
                        "Use your fingerprint, face, or device PIN to continue."
                }

                Text(
                    text = subtitleText,
                    fontSize = 13.sp,
                    color = if (!lockUiState.hasScreenLockOrBiometric) Color(0xFFB45309) else Color(0xFF6B7280),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (!isPasswordMode) {
                    Button(
                        onClick = { launchLevel1Unlock() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("vault_biometric_unlock_button"),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827)),
                        enabled = !unlockSuccessAnimating
                    ) {
                        SoftFingerprintIcon(size = 20.dp, color = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Unlock with Fingerprint or PIN",
                            color = Color.White,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { showPasswordForm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("vault_use_password_fallback_button"),
                        shape = RoundedCornerShape(50.dp),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Text(
                            text = "Use vault password",
                            color = Color(0xFF374151),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
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
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable { passwordVisible = !passwordVisible },
                                contentAlignment = Alignment.Center
                            ) {
                                SoftEyeIcon(
                                    visible = passwordVisible,
                                    size = 20.dp,
                                    tint = if (passwordVisible) Color(0xFF4F46E5) else Color(0xFF9CA3AF)
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
                            focusedBorderColor = Color(0xFF4F46E5),
                            focusedLabelColor = Color(0xFF4338CA),
                            cursorColor = Color(0xFF111827),
                            unfocusedBorderColor = Color(0xFFE5E7EB)
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitPasswordUnlock() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vault_password_input"),
                        shape = RoundedCornerShape(16.dp)
                    )

                    if (qaVaultPassword != null) {
                        TextButton(
                            onClick = {
                                password = qaVaultPassword
                                errorMessage = null
                            },
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("vault_fill_qa_password_button")
                        ) {
                            Text(
                                text = "Use QA Test Password",
                                fontSize = 12.sp,
                                color = Color(0xFF4F46E5),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    val displayError = if (cooldownSeconds > 0) {
                        "Too many failed attempts. Try again in ${cooldownSeconds}s."
                    } else {
                        errorMessage
                    }

                    displayError?.let { err ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFEF2F2), RoundedCornerShape(10.dp))
                                .border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SoftWarningIcon(size = 18.dp, color = Color(0xFFDC2626))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = err,
                                fontSize = 12.sp,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { submitPasswordUnlock() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("vault_unlock_button"),
                        shape = RoundedCornerShape(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827)),
                        enabled = !isLoading && cooldownSeconds == 0 && !unlockSuccessAnimating
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (cooldownSeconds > 0) "Locked (${cooldownSeconds}s)" else "Unlock Chats",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    if (!requiresPasswordOnly && activity != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                showPasswordForm = false
                                launchLevel1Unlock()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("vault_biometric_unlock_button"),
                            shape = RoundedCornerShape(50.dp),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            SoftFingerprintIcon(size = 20.dp, color = Color(0xFF374151))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Use Fingerprint or PIN",
                                color = Color(0xFF374151),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { showForgotOptionsDialog = true },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("vault_forgot_password_button")
                ) {
                    Text(
                        text = "Forgot password?",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showForgotOptionsDialog) {
        VaultForgotPasswordDialog(
            onDismiss = { showForgotOptionsDialog = false },
            onRestoreFromDrive = {
                showForgotOptionsDialog = false
                showRestoreBackupOverlay = true
            },
            onResetVault = {
                showForgotOptionsDialog = false
                showResetConfirmation = true
            }
        )
    }

    if (showRestoreBackupOverlay) {
        Dialog(
            onDismissRequest = { showRestoreBackupOverlay = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                RestoreChatBackupScreen(
                    onRestoreSuccess = {
                        showRestoreBackupOverlay = false
                        onDismiss()
                    },
                    onSkipRestore = { showRestoreBackupOverlay = false }
                )
            }
        }
    }

    if (showResetConfirmation) {
        Dialog(
            onDismissRequest = { showResetConfirmation = false },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
        ) {
            SecureWindow(enabled = true)
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                shadowElevation = 20.dp,
                border = BorderStroke(1.dp, Color(0xFFFEE2E2)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SoftIconBadge(
                        badgeSize = 56.dp,
                        backgroundColor = Color(0xFFFEE2E2),
                        borderColor = Color(0xFFFECACA)
                    ) {
                        SoftWarningIcon(size = 28.dp, color = Color(0xFFDC2626))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Reset Chat Vault?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Resetting the vault permanently wipes the local encryption keys and erases all chat history on this device.\n\nMessages cannot be recovered from the server.",
                        fontSize = 13.sp,
                        color = Color(0xFF4B5563),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            showResetConfirmation = false
                            VaultSecurityManager.resetVault(context)
                            Toast.makeText(context, "Vault reset. Please set up a new password.", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("vault_confirm_reset_button")
                    ) {
                        Text(
                            text = "Reset & Erase History",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { showResetConfirmation = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            color = Color(0xFF6B7280),
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VaultForgotPasswordDialog(
    onDismiss: () -> Unit,
    onRestoreFromDrive: () -> Unit,
    onResetVault: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        SecureWindow(enabled = true)
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.White,
            shadowElevation = 20.dp,
            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SoftIconBadge(
                    badgeSize = 56.dp,
                    backgroundColor = Color(0xFFEEF2FF),
                    borderColor = Color(0xFFC7D2FE)
                ) {
                    SoftShieldIcon(size = 28.dp, color = Color(0xFF4338CA))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Can't Remember Password?",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Your chat vault is end-to-end encrypted on this device, so FunkyTalk servers never store your password and cannot recover it.\n\nIf you backed up your chats, restore them from Google Drive. Otherwise, resetting the vault starts fresh and erases old local messages.",
                    fontSize = 13.sp,
                    color = Color(0xFF4B5563),
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = onRestoreFromDrive,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF111827)),
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("vault_forgot_restore_button")
                ) {
                    Text(
                        text = "Restore from my Google Drive backup",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onResetVault,
                    shape = RoundedCornerShape(50.dp),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("vault_forgot_reset_button")
                ) {
                    Text(
                        text = "Reset chat vault",
                        color = Color(0xFFDC2626),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(
                        text = "Cancel",
                        color = Color(0xFF6B7280),
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
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
        .setSubtitle("Confirm your fingerprint or face to protect your encrypted chats")
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
