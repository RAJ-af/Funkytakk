package com.example.ui.screens.profile

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.example.BuildConfig
import com.example.RecordChatBackupRequest
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.crypto.ChatBackupArchiveManager
import com.example.crypto.ChatBackupCryptoManager
import com.example.crypto.VaultSecurityManager
import com.example.service.ChatBackupWorker
import com.example.ui.components.GroupBackIcon
import com.example.ui.components.GroupLockIcon
import com.example.ui.components.GroupTrashIcon
import com.example.ui.components.Soft3DCloudIcon
import com.example.ui.components.Soft3DShieldIcon
import com.example.util.ChatBackupPreferencesManager
import com.example.util.GoogleDriveBackupHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBackupScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var googleAccountName by remember {
        mutableStateOf(GoogleDriveBackupHelper.getSignedInAccount(context)?.email)
    }
    var lastBackupTs by remember {
        mutableLongStateOf(ChatBackupPreferencesManager.getLastBackupTimestamp(context))
    }
    var lastBackupSize by remember {
        mutableLongStateOf(ChatBackupPreferencesManager.getLastBackupSizeBytes(context))
    }
    var autoBackupFreq by remember {
        mutableStateOf(ChatBackupPreferencesManager.getAutoBackupFrequency(context))
    }
    var includeMedia by remember {
        mutableStateOf(ChatBackupPreferencesManager.getIncludeMedia(context))
    }
    var wifiOnly by remember {
        mutableStateOf(ChatBackupPreferencesManager.getWifiOnly(context))
    }

    var isBackingUp by remember { mutableStateOf(false) }
    var backupStatusText by remember { mutableStateOf("") }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showPasswordPromptDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }

    // Launcher for Google Drive OAuth Sign-In
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
                    Toast.makeText(context, "Connected to Google Drive: ${account.email}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to connect Google Drive: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun initiateGoogleDriveSignIn() {
        val gso = GoogleDriveBackupHelper.getGoogleSignInOptions()
        val client = GoogleSignIn.getClient(context, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    fun executeBackupNow(password: String) {
        coroutineScope.launch {
            isBackingUp = true
            backupStatusText = "Connecting to Google Drive..."

            try {
                val account = GoogleDriveBackupHelper.getSignedInAccount(context)
                if (account == null) {
                    isBackingUp = false
                    Toast.makeText(context, "Please connect Google Drive first", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val token = GoogleDriveBackupHelper.fetchOAuthToken(context, account)
                if (token.isNullOrBlank()) {
                    isBackingUp = false
                    Toast.makeText(context, "Drive token expired. Please reconnect.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                backupStatusText = "Packing chat database & keys..."
                val vaultKey = VaultSecurityManager.getActiveVaultKey()
                val archiveBytes = ChatBackupArchiveManager.createArchive(context, includeMedia, vaultKey)

                backupStatusText = "Encrypting backup with AES-256-GCM..."
                val encryptedBytes = ChatBackupCryptoManager.encryptBackup(password, archiveBytes)

                backupStatusText = "Uploading to Google Drive appDataFolder..."
                val uploadResult = GoogleDriveBackupHelper.uploadBackupFile(token, encryptedBytes)

                if (uploadResult.isSuccess) {
                    val now = System.currentTimeMillis()
                    lastBackupTs = now
                    lastBackupSize = encryptedBytes.size.toLong()

                    ChatBackupPreferencesManager.setLastBackupTimestamp(context, now)
                    ChatBackupPreferencesManager.setLastBackupSizeBytes(context, lastBackupSize)
                    ChatBackupPreferencesManager.setCachedVaultPassword(context, password)

                    // Notify Supabase RPC
                    val supaAuth = SupabaseSessionManager.getBearerToken(context)
                    if (!supaAuth.isNullOrBlank()) {
                        try {
                            SupabaseClient.api.recordChatBackup(
                                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                                auth = supaAuth,
                                body = RecordChatBackupRequest(
                                    p_size_bytes = lastBackupSize,
                                    p_format_version = 1,
                                    p_includes_media = includeMedia,
                                    p_key_version = 1
                                )
                            )
                        } catch (e: Exception) {
                            // Non-fatal
                        }
                    }

                    Toast.makeText(context, "Chat backup created successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    val err = uploadResult.exceptionOrNull()?.message ?: "Upload failed"
                    Toast.makeText(context, "Backup error: $err", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error creating backup: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isBackingUp = false
                backupStatusText = ""
            }
        }
    }

    fun performBackupTrigger() {
        val cachedPassword = ChatBackupPreferencesManager.getCachedVaultPassword(context)
        if (!cachedPassword.isNullOrBlank()) {
            executeBackupNow(cachedPassword)
        } else {
            passwordInput = ""
            showPasswordPromptDialog = true
        }
    }

    fun executeDeleteBackup() {
        coroutineScope.launch {
            isBackingUp = true
            backupStatusText = "Deleting backup from Google Drive..."

            try {
                val account = GoogleDriveBackupHelper.getSignedInAccount(context)
                if (account != null) {
                    val token = GoogleDriveBackupHelper.fetchOAuthToken(context, account)
                    if (!token.isNullOrBlank()) {
                        val file = GoogleDriveBackupHelper.findBackupFileId(token)
                        if (file != null) {
                            GoogleDriveBackupHelper.deleteBackupFile(token, file.id)
                        }
                    }
                }

                // Notify Supabase
                val supaAuth = SupabaseSessionManager.getBearerToken(context)
                if (!supaAuth.isNullOrBlank()) {
                    try {
                        SupabaseClient.api.deleteChatBackupMeta(
                            apiKey = BuildConfig.SUPABASE_ANON_KEY,
                            auth = supaAuth
                        )
                    } catch (_: Exception) {}
                }

                ChatBackupPreferencesManager.setLastBackupTimestamp(context, 0L)
                ChatBackupPreferencesManager.setLastBackupSizeBytes(context, 0L)
                lastBackupTs = 0L
                lastBackupSize = 0L

                Toast.makeText(context, "Backup deleted successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error deleting backup: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isBackingUp = false
                backupStatusText = ""
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Chat Backup",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        GroupBackIcon(size = 22.dp, color = Color(0xFF0F172A))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Warning Banner: If you forget your vault password, backups cannot be recovered
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFFFBEB),
                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Soft3DShieldIcon(size = 22.dp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "End-to-End Encrypted Backups",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "If you forget your vault password, backups cannot be recovered.",
                            fontSize = 12.sp,
                            color = Color(0xFFB45309),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Google Drive Connection Status Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Soft3DCloudIcon(size = 28.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Google Drive Storage",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = if (googleAccountName != null) "Connected: $googleAccountName" else "Not connected",
                                fontSize = 13.sp,
                                color = if (googleAccountName != null) Color(0xFF16A34A) else Color(0xFF64748B)
                            )
                        }
                    }

                    Button(
                        onClick = { initiateGoogleDriveSignIn() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (googleAccountName != null) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                            contentColor = if (googleAccountName != null) Color(0xFF0F172A) else Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (googleAccountName != null) "Change Google Account" else "Connect Google Drive",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Backup Status & Manual "Back up now" Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Last Backup",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = if (lastBackupTs > 0) {
                                    SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(Date(lastBackupTs))
                                } else "Never",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Backup Size",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = if (lastBackupSize > 0) {
                                    String.format(Locale.US, "%.2f MB", lastBackupSize.toDouble() / (1024 * 1024))
                                } else "0 KB",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }

                    if (isBackingUp) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF0F172A))
                            Text(backupStatusText, fontSize = 13.sp, color = Color(0xFF475569))
                        }
                    }

                    Button(
                        onClick = { performBackupTrigger() },
                        enabled = !isBackingUp && googleAccountName != null,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Back up now",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Backup Settings & Options
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Backup Preferences",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    // Auto Backup Frequency selector
                    Column {
                        Text("Auto Backup Frequency", fontSize = 13.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("OFF", "DAILY", "WEEKLY").forEach { freq ->
                                val selected = autoBackupFreq == freq
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        autoBackupFreq = freq
                                        ChatBackupPreferencesManager.setAutoBackupFrequency(context, freq)
                                        ChatBackupWorker.scheduleAutoBackup(context)
                                    },
                                    label = { Text(freq.lowercase().replaceFirstChar { it.uppercase() }) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // Include Media toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Include Photos & Media", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                            Text("Back up images and voice notes", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                        Switch(
                            checked = includeMedia,
                            onCheckedChange = {
                                includeMedia = it
                                ChatBackupPreferencesManager.setIncludeMedia(context, it)
                            }
                        )
                    }

                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // Wi-Fi Only toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Back up over Wi-Fi only", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                            Text("Avoid cellular data usage", fontSize = 12.sp, color = Color(0xFF64748B))
                        }
                        Switch(
                            checked = wifiOnly,
                            onCheckedChange = {
                                wifiOnly = it
                                ChatBackupPreferencesManager.setWifiOnly(context, it)
                                ChatBackupWorker.scheduleAutoBackup(context)
                            }
                        )
                    }
                }
            }

            // Delete Backup Action
            if (lastBackupTs > 0) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Delete Chat Backup", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                            Text("Remove backup file from Google Drive", fontSize = 12.sp, color = Color(0xFFB91C1C))
                        }
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            GroupTrashIcon(size = 20.dp, color = Color(0xFFDC2626))
                        }
                    }
                }
            }
        }
    }

    // Vault Password Prompt Dialog
    if (showPasswordPromptDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordPromptDialog = false },
            title = { Text("Enter Vault Password", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Please enter your Secure Chat Vault password to encrypt the backup.")
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Vault Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (passwordInput.isNotBlank()) {
                            showPasswordPromptDialog = false
                            executeBackupNow(passwordInput)
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordPromptDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Backup?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete your encrypted chat backup from Google Drive?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        executeDeleteBackup()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
