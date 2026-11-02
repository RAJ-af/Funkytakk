package com.example.ui.screens.profile

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.AuthViewModel
import com.example.ui.components.Soft3DReportShield
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
            TopAppBar(
                title = {
                    Text(
                        "Delete Account",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFFFFDF5))
            )
        },
        containerColor = Color(0xFFFFFDF5)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Warning Badge
            Surface(
                color = Color(0xFFFEF2F2),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            "This action cannot be undone immediately",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF991B1B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Please read what will happen to your account, files, and personal data.",
                            fontSize = 12.sp,
                            color = Color(0xFFB91C1C),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Card 1: What will be deleted permanently
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "What will be deleted permanently:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "• Profile, photos, and banner\n" +
                        "• Chats, media files, and voice notes\n" +
                        "• Moments and comments\n" +
                        "• Followers and followings\n" +
                        "• Wallet, coins, and badges\n" +
                        "• Logged-in devices and your login itself",
                        fontSize = 13.sp,
                        color = Color(0xFF475569),
                        lineHeight = 20.sp
                    )
                }
            }

            // Card 2: What we keep
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Soft3DReportShield(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "What we keep:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF0F172A)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Only a minimal proof record of the deletion (receipt number, date, device, a one-way hash of the email; no messages, no photos).",
                        fontSize = 13.sp,
                        color = Color(0xFF475569),
                        lineHeight = 18.sp
                    )
                }
            }

            // Grace period notice (Dynamic pre-request: "a few days")
            Surface(
                color = Color(0xFFFEF9C3),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE047)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "Grace Period",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF854D0E)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Your account is hidden immediately and deleted for good after a few days. Sign in again during that time to cancel.",
                        fontSize = 12.sp,
                        color = Color(0xFF713F12),
                        lineHeight = 17.sp
                    )
                }
            }

            // Reason Chips
            Column {
                Text(
                    "Why are you leaving? (Optional)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFF334155)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) Color(0xFFFEE2E2) else Color.White,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) Color(0xFFDC2626) else Color(0xFFCBD5E1)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = reason,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFFDC2626) else Color(0xFF475569),
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                                    )
                                }
                            }
                            if (rowReasons.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Erase Local Chats Switch
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Erase local chats immediately",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            "Also erase my chats stored on this phone now (cannot be undone, even if I cancel later)",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 15.sp
                        )
                    }
                    Switch(
                        checked = eraseLocalChats,
                        onCheckedChange = { eraseLocalChats = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFDC2626)
                        )
                    )
                }
            }

            // Confirm by typing DELETE
            Column {
                Text(
                    "Type DELETE to confirm:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFF334155)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = confirmationInput,
                    onValueChange = { confirmationInput = it },
                    placeholder = { Text("DELETE", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedBorderColor = Color(0xFFDC2626),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }

            errorMessage?.let { err ->
                Text(
                    text = err,
                    color = Color(0xFFDC2626),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Delete Button
            Button(
                onClick = { executeDeletion() },
                enabled = isDeleteConfirmed && !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626),
                    disabledContainerColor = Color(0xFFFDA4AF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        "Delete my account",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Re-auth Password Dialog
    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isReauthenticating) showPasswordDialog = false
            },
            title = {
                Text("Confirm Your Password", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column {
                    Text(
                        "For your security, please confirm your password to proceed with account deletion.",
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        placeholder = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A)
                        )
                    )
                    reauthError?.let { err ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(err, color = Color(0xFFDC2626), fontSize = 12.sp)
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    if (isReauthenticating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Confirm & Delete", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPasswordDialog = false },
                    enabled = !isReauthenticating
                ) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }
}
