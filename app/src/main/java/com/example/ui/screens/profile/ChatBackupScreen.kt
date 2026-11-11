package com.example.ui.screens.profile

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.crypto.DriveBackupEngine
import com.example.service.ChatBackupScheduler
import com.example.ui.components.Soft3DCloudIcon
import com.example.ui.components.Soft3DShieldIcon
import com.example.util.ChatBackupPreferencesManager
import com.example.util.StorageQuotaExceededException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatBackupScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var driveAccountName by remember {
        mutableStateOf(ChatBackupPreferencesManager.getDriveAccountName(context))
    }
    var isReconnectNeeded by remember { mutableStateOf(false) }

    var lastBackupTimestamp by remember {
        mutableLongStateOf(ChatBackupPreferencesManager.getLastBackupTimestamp(context))
    }
    var lastBackupSizeBytes by remember {
        mutableLongStateOf(ChatBackupPreferencesManager.getLastBackupSizeBytes(context))
    }

    var autoBackupFreq by remember {
        mutableStateOf(ChatBackupPreferencesManager.getAutoBackupFrequency(context).let {
            when (it.uppercase()) {
                "DAILY" -> "Daily"
                "WEEKLY" -> "Weekly"
                else -> "Off"
            }
        })
    }
    var includePhotos by remember {
        mutableStateOf(ChatBackupPreferencesManager.getIncludeMedia(context))
    }
    var wifiOnly by remember {
        mutableStateOf(ChatBackupPreferencesManager.getWifiOnly(context))
    }

    var isBackingUp by remember { mutableStateOf(false) }
    var backupProgressPercent by remember { mutableIntStateOf(0) }
    var backupProgressText by remember { mutableStateOf("") }
    var backupCancellationRequested by remember { mutableStateOf(false) }

    var userErrorMessage by remember { mutableStateOf<String?>(null) }

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(Exception::class.java)
                if (account != null && !account.email.isNullOrBlank()) {
                    driveAccountName = account.email
                    isReconnectNeeded = false
                    ChatBackupPreferencesManager.setDriveAccountName(context, account.email)
                    Toast.makeText(context, "Connected: ${account.email}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Google Drive sign-in failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun initiateDriveConnection() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.appdata"))
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        googleSignInLauncher.launch(client.signInIntent)
    }

    fun triggerBackupNow() {
        if (driveAccountName.isNullOrBlank()) {
            initiateDriveConnection()
            return
        }

        coroutineScope.launch {
            isBackingUp = true
            backupCancellationRequested = false
            backupProgressPercent = 0
            backupProgressText = "Preparing backup..."
            userErrorMessage = null

            try {
                val success = DriveBackupEngine.performBackup(
                    context = context,
                    onProgress = { percent, text ->
                        backupProgressPercent = percent
                        backupProgressText = text
                    },
                    isCancelled = { backupCancellationRequested }
                )

                if (success) {
                    lastBackupTimestamp = ChatBackupPreferencesManager.getLastBackupTimestamp(context)
                    lastBackupSizeBytes = ChatBackupPreferencesManager.getLastBackupSizeBytes(context)
                    Toast.makeText(context, "Chat backup finished successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Backup cancelled.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: StorageQuotaExceededException) {
                userErrorMessage = "Your Google Drive is full. Please free up space to back up your chats."
            } catch (e: SecurityException) {
                isReconnectNeeded = true
                userErrorMessage = "Google Drive access expired. Please reconnect."
            } catch (e: IOException) {
                userErrorMessage = "No network connection. Please check your internet connection."
            } catch (e: Exception) {
                val msg = e.message.orEmpty()
                if (msg.contains("quota", ignoreCase = true)) {
                    userErrorMessage = "Your Google Drive is full. Please free up space to back up your chats."
                } else if (msg.contains("network", ignoreCase = true) || msg.contains("connect", ignoreCase = true)) {
                    userErrorMessage = "No network connection. Please check your internet connection."
                } else {
                    userErrorMessage = "Backup failed: ${e.message}"
                }
            } finally {
                isBackingUp = false
                backupCancellationRequested = false
            }
        }
    }

    val formattedLastBackupTime = remember(lastBackupTimestamp) {
        if (lastBackupTimestamp <= 0L) {
            "Never"
        } else {
            val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            sdf.format(Date(lastBackupTimestamp))
        }
    }

    val formattedBackupSize = remember(lastBackupSizeBytes) {
        if (lastBackupSizeBytes <= 0L) {
            "0 KB"
        } else {
            val mb = lastBackupSizeBytes.toDouble() / (1024 * 1024)
            if (mb < 0.1) {
                val kb = lastBackupSizeBytes / 1024
                "$kb KB"
            } else {
                String.format(Locale.US, "%.2f MB", mb)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDF5))
            .verticalScroll(rememberScrollState())
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF0F172A),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Chat ", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                    Text("Backup", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFFFB800))
                }
                Text(
                    text = "Keep your chats safe and accessible across devices.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }

            // Top 3D Cloud Upload Icon
            Soft3DCloudIcon(size = 64.dp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 1: End-to-End Encrypted Backups Warning
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFFEF08A).copy(alpha = 0.4f))
                .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Soft3DShieldIcon(size = 54.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("End-to-End Encrypted Backups", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your backups are encrypted. If you forget your vault password, backups cannot be recovered.",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        if (userErrorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626))
                    Text(
                        text = userErrorMessage!!,
                        fontSize = 13.sp,
                        color = Color(0xFF991B1B),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 2: Google Drive Storage
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Google Drive Storage", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        val statusText = when {
                            isReconnectNeeded -> "Reconnect needed"
                            !driveAccountName.isNullOrBlank() -> "Connected: $driveAccountName"
                            else -> "Not connected"
                        }
                        val statusColor = when {
                            isReconnectNeeded -> Color(0xFFDC2626)
                            !driveAccountName.isNullOrBlank() -> Color(0xFF16A34A)
                            else -> Color(0xFF64748B)
                        }
                        Text(statusText, fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.Medium)
                    }

                    Image(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google Drive",
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (driveAccountName.isNullOrBlank() || isReconnectNeeded) {
                    // Connect / Reconnect Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFFFFD000), Color(0xFFFFB800)))
                            )
                            .clickable { initiateDriveConnection() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isReconnectNeeded) "Reconnect Google Drive" else "Connect Google Drive",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    // Connected actions: Change Password, Disconnect, Delete Backup
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showChangePasswordDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Change password", fontSize = 11.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                driveAccountName = null
                                ChatBackupPreferencesManager.setDriveAccountName(context, null)
                                Toast.makeText(context, "Google Drive disconnected.", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Disconnect", fontSize = 11.sp, color = Color(0xFFDC2626), maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Delete backup from Google Drive", fontSize = 12.sp, color = Color(0xFFDC2626))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 3: Last Backup Stats
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).background(Color(0xFFFEF08A), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF854D0E), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Last Backup", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(formattedLastBackupTime, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }

                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).background(Color(0xFFFEF08A), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF854D0E), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Backup Size", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(formattedBackupSize, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isBackingUp) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = backupProgressText,
                                fontSize = 12.sp,
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$backupProgressPercent%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }

                        LinearProgressIndicator(
                            progress = { backupProgressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFFFFB800),
                            trackColor = Color(0xFFF1F5F9)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedButton(
                            onClick = {
                                backupCancellationRequested = true
                                backupProgressText = "Cancelling..."
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel Backup", color = Color(0xFFDC2626), fontSize = 13.sp)
                        }
                    }
                } else {
                    // Back up now button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFFFFD000), Color(0xFFFFB800)))
                            )
                            .clickable { triggerBackupNow() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Back up now", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 4: Backup Preferences
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column {
                Text("Backup Preferences", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text("Customize how your chats are backed up.", fontSize = 12.sp, color = Color(0xFF64748B))

                Spacer(modifier = Modifier.height(14.dp))

                Text("Auto Backup Frequency", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Off", "Daily", "Weekly").forEach { freq ->
                        val isSelected = autoBackupFreq == freq
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(19.dp))
                                .background(if (isSelected) Color(0xFFFFD000) else Color(0xFFF8FAFC))
                                .border(1.dp, if (isSelected) Color(0xFFFFD000) else Color(0xFFE2E8F0), RoundedCornerShape(19.dp))
                                .clickable {
                                    autoBackupFreq = freq
                                    val prefFreq = freq.uppercase()
                                    ChatBackupPreferencesManager.setAutoBackupFrequency(context, prefFreq)
                                    ChatBackupScheduler.updateBackupSchedule(context)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(freq, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Switch 1: Include Photos & Media
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Include Photos & Media", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                        Text("Back up images, videos, voice notes and other media files.", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = includePhotos,
                        onCheckedChange = {
                            includePhotos = it
                            ChatBackupPreferencesManager.setIncludeMedia(context, it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFFB800))
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Switch 2: Back up over Wi-Fi only
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Back up over Wi-Fi only", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                        Text("Avoid using cellular data for backups.", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = wifiOnly,
                        onCheckedChange = {
                            wifiOnly = it
                            ChatBackupPreferencesManager.setWifiOnly(context, it)
                            ChatBackupScheduler.updateBackupSchedule(context)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFFB800))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tip Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFEEF2FF))
                .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFF4F46E5), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tip", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3730A3))
                    Text(
                        "Regular backups keep your conversations safe, even if you change or lose your device.",
                        fontSize = 11.sp,
                        color = Color(0xFF4338CA),
                        lineHeight = 15.sp
                    )
                }
                Soft3DCloudIcon(size = 48.dp)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }

    // Dialog: Change Backup Password (Re-wrap only)
    if (showChangePasswordDialog) {
        var currentPasswordInput by remember { mutableStateOf("") }
        var newPasswordInput by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }
        var dialogError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showChangePasswordDialog = false },
            title = { Text("Change Backup Password", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "This will re-encrypt your backup master key with a new vault password without re-uploading your chat data.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    OutlinedTextField(
                        value = currentPasswordInput,
                        onValueChange = { currentPasswordInput = it },
                        label = { Text("Current Vault Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("New Vault Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (dialogError != null) {
                        Text(dialogError!!, color = Color(0xFFDC2626), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (currentPasswordInput.isBlank() || newPasswordInput.isBlank()) {
                            dialogError = "Both fields are required."
                            return@Button
                        }
                        if (newPasswordInput.length < 4) {
                            dialogError = "New password must be at least 4 characters."
                            return@Button
                        }

                        coroutineScope.launch {
                            isSubmitting = true
                            dialogError = null
                            try {
                                DriveBackupEngine.changeBackupPassword(
                                    context = context,
                                    oldPassword = currentPasswordInput,
                                    newPassword = newPasswordInput
                                )
                                Toast.makeText(context, "Backup password updated successfully!", Toast.LENGTH_SHORT).show()
                                showChangePasswordDialog = false
                            } catch (e: Exception) {
                                dialogError = "Failed: ${e.message}"
                            } finally {
                                isSubmitting = false
                            }
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB800))
                ) {
                    Text("Update Password", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showChangePasswordDialog = false },
                    enabled = !isSubmitting
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Delete Backup From Google Drive
    if (showDeleteConfirmDialog) {
        var isDeleting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteConfirmDialog = false },
            title = { Text("Delete Backup from Drive?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete your backup from Google Drive? This will permanently delete all encrypted chat backups and media from Google Drive. This action cannot be undone.",
                    fontSize = 13.sp,
                    color = Color(0xFF475569),
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isDeleting = true
                            try {
                                DriveBackupEngine.deleteBackupFromDrive(context)
                                lastBackupTimestamp = 0L
                                lastBackupSizeBytes = 0L
                                Toast.makeText(context, "Backup deleted from Google Drive.", Toast.LENGTH_SHORT).show()
                                showDeleteConfirmDialog = false
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to delete backup: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isDeleting = false
                            }
                        }
                    },
                    enabled = !isDeleting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete Backup", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    enabled = !isDeleting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
