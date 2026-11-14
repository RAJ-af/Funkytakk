package com.example.ui.screens.profile

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AuthViewModel
import com.example.R
import com.example.ui.components.SoftEyeIcon
import com.example.ui.components.SoftShieldIcon
import com.example.ui.components.SoftWarningIcon
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.LocalFunkyColors
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private data class DeletionImpactItem(
    @DrawableRes val iconRes: Int,
    val title: String,
    val detail: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDeletionScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    onDeletionScheduled: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val funkyColors = LocalFunkyColors.current
    val isDark = LocalAppDarkTheme.current

    var selectedReason by remember { mutableStateOf<String?>(null) }
    var confirmationInput by remember { mutableStateOf("") }
    var eraseLocalChats by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Reauth Dialog State
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var reauthError by remember { mutableStateOf<String?>(null) }
    var isReauthenticating by remember { mutableStateOf(false) }

    val reasons = listOf(
        "Taking a break",
        "Privacy concerns",
        "Created a new account",
        "Too distracting",
        "Other"
    )

    val impactItems = remember {
        listOf(
            DeletionImpactItem(R.drawable.ic_profile, "Profile & Identity", "Display name, avatar, banner, bio & badges"),
            DeletionImpactItem(R.drawable.ic_chats, "Chats & Voice Notes", "Private messages, voice recordings & shared files"),
            DeletionImpactItem(R.drawable.ic_photo, "Moments & Comments", "All shared posts, captions, fixes & reactions"),
            DeletionImpactItem(R.drawable.ic_coin, "Wallet & Balances", "Coins, VIP cosmetics, frames & room perks"),
            DeletionImpactItem(R.drawable.ic_lock, "Active Sessions", "Logged-in devices and account credentials")
        )
    }

    val isDeleteConfirmed = confirmationInput.trim().equals("DELETE", ignoreCase = false)

    fun executeDeletion() {
        isLoading = true
        errorMessage = null
        authViewModel.requestAccountDeletion(
            reason = selectedReason,
            eraseLocalChats = eraseLocalChats,
            context = context,
            onReauthRequired = {
                isLoading = false
                val fbUser = FirebaseAuth.getInstance().currentUser
                val isGoogleUser = fbUser?.providerData?.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } == true
                if (isGoogleUser) {
                    Toast.makeText(context, "Please re-authenticate with your Google account", Toast.LENGTH_LONG).show()
                } else {
                    passwordInput = ""
                    reauthError = null
                    showPasswordDialog = true
                }
            },
            onSuccess = {
                isLoading = false
                onDeletionScheduled()
            },
            onError = { err ->
                isLoading = false
                errorMessage = err
                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
            }
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(funkyColors.bg)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        onClick = onBack,
                        shape = CircleShape,
                        color = funkyColors.surface,
                        border = BorderStroke(1.dp, funkyColors.line),
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("delete_account_back_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back),
                                contentDescription = "Back",
                                tint = funkyColors.ink,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Delete Account",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp,
                            color = funkyColors.ink
                        )
                        Text(
                            text = "Account removal & data controls",
                            fontSize = 12.sp,
                            color = funkyColors.mute
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = funkyColors.danger.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, funkyColors.danger.copy(alpha = 0.30f))
                ) {
                    Text(
                        text = "Sensitive",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = funkyColors.danger,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        },
        containerColor = funkyColors.bg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Danger Banner Card
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = funkyColors.surface,
                border = BorderStroke(1.dp, funkyColors.danger.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    funkyColors.danger.copy(alpha = if (isDark) 0.18f else 0.08f),
                                    funkyColors.surface
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(funkyColors.danger.copy(alpha = 0.15f))
                                .border(1.dp, funkyColors.danger.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            SoftWarningIcon(size = 26.dp, color = funkyColors.danger)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Before you say goodbye",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = funkyColors.ink
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Deleting your account hides your profile immediately and permanently erases personal data once the recovery window ends.",
                                fontSize = 12.5.sp,
                                color = funkyColors.mute,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Card 1: What will be permanently erased
            Surface(
                color = funkyColors.surface,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, funkyColors.line),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "What will be deleted permanently",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = funkyColors.ink
                    )
                    impactItems.forEachIndexed { idx, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(funkyColors.soft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = item.iconRes),
                                    contentDescription = null,
                                    tint = funkyColors.danger,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = funkyColors.ink
                                )
                                Text(
                                    text = item.detail,
                                    fontSize = 12.sp,
                                    color = funkyColors.mute
                                )
                            }
                        }
                        if (idx < impactItems.lastIndex) {
                            HorizontalDivider(color = funkyColors.line.copy(alpha = 0.55f))
                        }
                    }
                }
            }

            // Card 2: Audit record & Recovery Grace Period
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = funkyColors.surface,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, funkyColors.line),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SoftShieldIcon(size = 24.dp, showCheck = true)
                        Text(
                            text = "What we keep",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = funkyColors.ink
                        )
                        Text(
                            text = "Minimal legal receipt (ID, timestamp, one-way email hash; zero messages or photos).",
                            fontSize = 11.5.sp,
                            color = funkyColors.mute,
                            lineHeight = 16.sp
                        )
                    }
                }

                Surface(
                    color = funkyColors.mango.copy(alpha = if (isDark) 0.14f else 0.16f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, funkyColors.mango.copy(alpha = 0.45f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(funkyColors.mango),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_refresh),
                                contentDescription = null,
                                tint = funkyColors.onMango,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "Grace Period",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = funkyColors.ink
                        )
                        Text(
                            text = "Account stays hidden for a few days. Sign back in anytime during this window to cancel.",
                            fontSize = 11.5.sp,
                            color = funkyColors.ink.copy(alpha = 0.80f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Reason Chips
            Surface(
                color = funkyColors.surface,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, funkyColors.line),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Why are you leaving? (Optional)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = funkyColors.ink
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        reasons.chunked(2).forEach { rowReasons ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowReasons.forEach { reason ->
                                    val isSelected = selectedReason == reason
                                    Surface(
                                        onClick = {
                                            selectedReason = if (isSelected) null else reason
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isSelected) funkyColors.danger.copy(alpha = 0.14f) else funkyColors.soft,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) funkyColors.danger else funkyColors.line
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 11.dp, horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = reason,
                                                fontSize = 12.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) funkyColors.danger else funkyColors.ink,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.ic_check),
                                                    contentDescription = null,
                                                    tint = funkyColors.danger,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (rowReasons.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Erase Local Chats Switch
            Surface(
                color = funkyColors.surface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, funkyColors.line),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(funkyColors.danger.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_trash),
                            contentDescription = null,
                            tint = funkyColors.danger,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Erase local chats immediately",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = funkyColors.ink
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Wipe messages stored on this phone now (cannot be undone even if you cancel later).",
                            fontSize = 11.5.sp,
                            color = funkyColors.mute,
                            lineHeight = 15.sp
                        )
                    }
                    Switch(
                        checked = eraseLocalChats,
                        onCheckedChange = { eraseLocalChats = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = funkyColors.danger,
                            uncheckedThumbColor = funkyColors.mute,
                            uncheckedTrackColor = funkyColors.soft,
                            uncheckedBorderColor = funkyColors.line
                        )
                    )
                }
            }

            // Final Typed Confirmation Card
            Surface(
                color = funkyColors.surface,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(
                    1.dp,
                    if (isDeleteConfirmed) funkyColors.ok.copy(alpha = 0.6f) else funkyColors.line
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Type DELETE to confirm",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = funkyColors.ink
                        )
                        if (isDeleteConfirmed) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check),
                                    contentDescription = "Confirmed",
                                    tint = funkyColors.ok,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Ready",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = funkyColors.ok
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmationInput,
                        onValueChange = { confirmationInput = it },
                        placeholder = { Text("DELETE", color = funkyColors.mute) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_account_confirm_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = funkyColors.ink,
                            unfocusedTextColor = funkyColors.ink,
                            focusedBorderColor = if (isDeleteConfirmed) funkyColors.ok else funkyColors.danger,
                            unfocusedBorderColor = funkyColors.line,
                            focusedContainerColor = funkyColors.soft,
                            unfocusedContainerColor = funkyColors.soft,
                            cursorColor = funkyColors.danger
                        )
                    )
                }
            }

            errorMessage?.let { err ->
                Surface(
                    color = funkyColors.danger.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = err,
                        color = funkyColors.danger,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Primary Delete CTA Button
            Button(
                onClick = { executeDeletion() },
                enabled = isDeleteConfirmed && !isLoading,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = funkyColors.danger,
                    disabledContainerColor = funkyColors.danger.copy(alpha = 0.28f),
                    disabledContentColor = Color.White.copy(alpha = 0.65f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("delete_account_submit_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_trash),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Delete my account",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Re-auth Password Dialog
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isReauthenticating) showPasswordDialog = false
            },
            containerColor = funkyColors.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(funkyColors.danger.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_lock),
                            contentDescription = null,
                            tint = funkyColors.danger,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Confirm Password",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = funkyColors.ink
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "For your security, please verify your password before scheduling account deletion.",
                        fontSize = 13.sp,
                        color = funkyColors.mute,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        placeholder = { Text("Enter password", color = funkyColors.mute) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                SoftEyeIcon(
                                    visible = passwordVisible,
                                    size = 20.dp,
                                    tint = funkyColors.mute
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = funkyColors.ink,
                            unfocusedTextColor = funkyColors.ink,
                            focusedBorderColor = funkyColors.danger,
                            unfocusedBorderColor = funkyColors.line,
                            focusedContainerColor = funkyColors.soft,
                            unfocusedContainerColor = funkyColors.soft,
                            cursorColor = funkyColors.danger
                        )
                    )
                    reauthError?.let { err ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(err, color = funkyColors.danger, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fbUser = FirebaseAuth.getInstance().currentUser
                        val email = fbUser?.email
                        if (email.isNullOrBlank() || passwordInput.isBlank()) {
                            reauthError = "Please enter your password"
                            return@Button
                        }
                        isReauthenticating = true
                        reauthError = null
                        coroutineScope.launch {
                            try {
                                val cred = EmailAuthProvider.getCredential(email, passwordInput)
                                fbUser.reauthenticate(cred).await()
                                isReauthenticating = false
                                showPasswordDialog = false
                                executeDeletion()
                            } catch (e: Exception) {
                                isReauthenticating = false
                                reauthError = "Invalid password. Please try again."
                            }
                        }
                    },
                    enabled = !isReauthenticating && passwordInput.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = funkyColors.danger)
                ) {
                    if (isReauthenticating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Confirm & Delete", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPasswordDialog = false },
                    enabled = !isReauthenticating
                ) {
                    Text("Cancel", color = funkyColors.mute, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}
