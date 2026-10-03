package com.example.ui.screens.chat

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ChatBackupStatusResponse
import com.example.crypto.ChatBackupArchiveManager
import com.example.crypto.ChatBackupCryptoManager
import com.example.crypto.VaultSecurityManager
import com.example.ui.components.GroupLockIcon
import com.example.ui.components.Soft3DCloudIcon
import com.example.ui.components.Soft3DShieldIcon
import com.example.util.ChatBackupPreferencesManager
import com.example.util.GoogleDriveBackupHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RestoreChatBackupScreen(
    backupStatus: ChatBackupStatusResponse,
    onRestoreSuccess: () -> Unit,
    onSkipRestore: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var googleAccountName by remember {
        mutableStateOf(GoogleDriveBackupHelper.getSignedInAccount(context)?.email)
    }
    var vaultPasswordInput by remember { mutableStateOf("") }
    var isRestoring by remember { mutableStateOf(false) }
    var restoreStatusText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryDelaySeconds by remember { mutableIntStateOf(0) }
    var failedAttemptsCount by remember { mutableIntStateOf(0) }
    var showSkipWarningDialog by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(Exception::class.java)
                if (account != null) {
                    googleAccountName = account.email
                    ChatBackupPreferencesManager.setDriveAccountName(context, account.email)
                    Toast.makeText(context, "Connected Google Drive: ${account.email}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to connect Google Drive: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun initiateDriveConnection() {
        val gso = GoogleDriveBackupHelper.getGoogleSignInOptions()
        val client = GoogleSignIn.getClient(context, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    fun executeRestore() {
        if (vaultPasswordInput.isBlank()) {
            errorMessage = "Please enter your vault password."
            return
        }

        coroutineScope.launch {
            isRestoring = true
            errorMessage = null
            restoreStatusText = "Connecting to Google Drive..."

            try {
                val account = GoogleDriveBackupHelper.getSignedInAccount(context)
                if (account == null) {
                    isRestoring = false
                    errorMessage = "Please connect your Google Drive account first."
                    return@launch
                }

                val token = GoogleDriveBackupHelper.fetchOAuthToken(context, account)
                if (token.isNullOrBlank()) {
                    isRestoring = false
                    errorMessage = "Drive authorization failed. Please reconnect."
                    return@launch
                }

                restoreStatusText = "Searching for chat backup archive..."
                val backupFile = GoogleDriveBackupHelper.findBackupFileId(token)
                if (backupFile == null) {
                    isRestoring = false
                    errorMessage = "No backup file found in Google Drive appDataFolder."
                    return@launch
                }

                restoreStatusText = "Downloading encrypted chat backup..."
                val downloadResult = GoogleDriveBackupHelper.downloadBackupFile(token, backupFile.id)
                if (downloadResult.isFailure) {
                    isRestoring = false
                    errorMessage = "Failed to download backup file: ${downloadResult.exceptionOrNull()?.message}"
                    return@launch
                }

                val encryptedBytes = downloadResult.getOrThrow()

                restoreStatusText = "Decrypting chat backup..."
                val decryptedArchiveBytes = try {
                    ChatBackupCryptoManager.decryptBackup(vaultPasswordInput, encryptedBytes)
                } catch (e: IllegalArgumentException) {
                    failedAttemptsCount++
                    val delayMs = (failedAttemptsCount * 1000L).coerceAtMost(5000L)
                    restoreStatusText = "Wrong password. Retrying in ${delayMs / 1000}s..."
                    delay(delayMs)
                    isRestoring = false
                    errorMessage = "Wrong vault password. Please try again."
                    return@launch
                }

                restoreStatusText = "Restoring database & encryption keys..."
                val restoreSuccess = ChatBackupArchiveManager.restoreFromArchive(
                    context = context,
                    archiveBytes = decryptedArchiveBytes,
                    vaultPassword = vaultPasswordInput
                )

                if (restoreSuccess) {
                    ChatBackupPreferencesManager.setCachedVaultPassword(context, vaultPasswordInput)
                    ChatBackupPreferencesManager.setLastBackupTimestamp(context, System.currentTimeMillis())
                    Toast.makeText(context, "Chats & keys restored successfully!", Toast.LENGTH_SHORT).show()
                    onRestoreSuccess()
                } else {
                    errorMessage = "Failed to unpack and restore chat database."
                }
            } catch (e: Exception) {
                errorMessage = "Restore error: ${e.message}"
            } finally {
                isRestoring = false
                restoreStatusText = ""
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Header Icon Badge
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFFEF3C7)),
                contentAlignment = Alignment.Center
            ) {
                Soft3DCloudIcon(size = 40.dp)
            }

            Text(
                text = "Restore Encrypted Chats",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )

            Text(
                text = "We found an encrypted chat backup from your Google Drive account. Connect Drive and enter your Vault password to restore your conversations.",
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Key Version Warning Banner if backup key version < current key version
            if (backupStatus.backup_key_version < backupStatus.current_key_version) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Soft3DShieldIcon(size = 22.dp)
                        Text(
                            text = "Warning: The backup is older than your current keys. Some messages may not decrypt.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFB45309),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Backup Details Summary Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Backup Date", fontSize = 13.sp, color = Color(0xFF64748B))
                        Text(
                            text = if (!backupStatus.last_backup_at.isNullOrBlank()) {
                                backupStatus.last_backup_at
                            } else "Available on Drive",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Backup Size", fontSize = 13.sp, color = Color(0xFF64748B))
                        Text(
                            text = if (backupStatus.size_bytes > 0) {
                                String.format(Locale.US, "%.2f MB", backupStatus.size_bytes.toDouble() / (1024 * 1024))
                            } else "Unknown size",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }

            // Step 1: Connect Google Drive
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Connect Google Account",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Button(
                        onClick = { initiateDriveConnection() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (googleAccountName != null) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                            contentColor = if (googleAccountName != null) Color(0xFF0F172A) else Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (googleAccountName != null) "Connected: $googleAccountName" else "Connect Google Drive",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Step 2: Vault Password Input
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "2. Secure Chat Vault Password",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    OutlinedTextField(
                        value = vaultPasswordInput,
                        onValueChange = {
                            vaultPasswordInput = it
                            errorMessage = null
                        },
                        label = { Text("Vault Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        leadingIcon = { GroupLockIcon(size = 20.dp, color = Color(0xFF64748B)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (!errorMessage.isNullOrBlank()) {
                        Text(
                            text = errorMessage.orEmpty(),
                            fontSize = 12.sp,
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (isRestoring) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color(0xFF0F172A))
                    Text(restoreStatusText, fontSize = 13.sp, color = Color(0xFF475569))
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { executeRestore() },
                    enabled = !isRestoring && googleAccountName != null && vaultPasswordInput.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF59E0B),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Restore Chats Now", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { showSkipWarningDialog = true },
                    enabled = !isRestoring,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Skip Restore", fontSize = 15.sp, color = Color(0xFF64748B))
                }
            }
        }
    }

    // Skip Confirmation Warning Dialog
    if (showSkipWarningDialog) {
        AlertDialog(
            onDismissRequest = { showSkipWarningDialog = false },
            title = { Text("Skip Chat Restore?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "If you skip restoring, a new empty vault will be initialized. Previous local chat history and encrypted messages will be permanently lost on this device.",
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSkipWarningDialog = false
                        onSkipRestore()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Skip & Start Fresh")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSkipWarningDialog = false }) {
                    Text("Back to Restore")
                }
            }
        )
    }
}
