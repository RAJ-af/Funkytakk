package com.example.ui.screens.profile

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.crypto.BackupCrypto
import com.example.crypto.DriveBackupEngine
import com.example.crypto.VaultSecurityManager
import com.example.service.ChatBackupScheduler
import com.example.ui.components.FunkyLightbulbIcon
import com.example.ui.components.Soft3DCloudIcon
import com.example.ui.components.Soft3DShieldIcon
import com.example.ui.components.SoftArrowRightIcon
import com.example.ui.components.SoftBackIcon
import com.example.ui.components.SoftCloseIcon
import com.example.ui.components.SoftEyeIcon
import com.example.ui.components.SoftWarningIcon
import com.example.ui.theme.LocalFunkyColors
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
    val funkyColors = LocalFunkyColors.current
    val isDark = funkyColors.isDark
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

    var showProtectBackupDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    fun triggerBackupNow() {
        if (driveAccountName.isNullOrBlank()) {
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
                    userErrorMessage = null
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
                } else if (msg.contains("network", ignoreCase = true) || msg.contains("connect", ignoreCase = true) || msg.contains("timeout", ignoreCase = true)) {
                    userErrorMessage = "No network connection. Please check your internet connection."
                } else if (msg.contains("401", ignoreCase = true) || msg.contains("unauthorized", ignoreCase = true) || msg.contains("expired", ignoreCase = true)) {
                    isReconnectNeeded = true
                    userErrorMessage = "Google Drive access expired. Please reconnect."
                } else if (msg.contains("password", ignoreCase = true) || msg.contains("key", ignoreCase = true) || msg.contains("header", ignoreCase = true)) {
                    showProtectBackupDialog = true
                    userErrorMessage = "Backup password setup required. Please protect your backup."
                } else {
                    userErrorMessage = "Backup could not be completed. Please check your connection and try again."
                }
            } finally {
                isBackingUp = false
                backupCancellationRequested = false
            }
        }
    }

    fun onBackUpNowClicked() {
        if (driveAccountName.isNullOrBlank()) {
            return
        }

        val email = driveAccountName!!
        val hasLocalBk = BackupCrypto.loadMasterKeyFromKeystore(context) != null
        if (!hasLocalBk) {
            showProtectBackupDialog = true
            return
        }

        coroutineScope.launch {
            val hasHeader = DriveBackupEngine.hasBackupHeaderOnDrive(context, email)
            if (!hasHeader) {
                showProtectBackupDialog = true
            } else {
                triggerBackupNow()
            }
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(Exception::class.java)
                if (account != null && !account.email.isNullOrBlank()) {
                    val emailStr = account.email!!
                    driveAccountName = emailStr
                    isReconnectNeeded = false
                    ChatBackupPreferencesManager.setDriveAccountName(context, emailStr)
                    Toast.makeText(context, "Connected: $emailStr", Toast.LENGTH_SHORT).show()

                    coroutineScope.launch {
                        val hasLocalBk = BackupCrypto.loadMasterKeyFromKeystore(context) != null
                        val hasHeader = DriveBackupEngine.hasBackupHeaderOnDrive(context, emailStr)
                        if (!hasLocalBk || !hasHeader) {
                            showProtectBackupDialog = true
                        }
                    }
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
            .background(funkyColors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
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
                    .background(funkyColors.surface)
                    .border(1.dp, funkyColors.line, CircleShape)
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                SoftBackIcon(
                    color = funkyColors.ink,
                    size = 18.dp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Chat ", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = funkyColors.ink)
                    Text("Backup", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = funkyColors.mango)
                }
                Text(
                    text = "Keep your chats safe and accessible across devices.",
                    fontSize = 12.sp,
                    color = funkyColors.mute
                )
            }

            Soft3DCloudIcon(size = 64.dp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card 1: End-to-End Encrypted Backups Warning
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(funkyColors.soft)
                .border(1.dp, funkyColors.mango.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
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
                        Text("End-to-End Encrypted Backups", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = funkyColors.ink)
                        BackupInfoGraphic(color = funkyColors.mute, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your backups are encrypted. If you forget your vault password, backups cannot be recovered.",
                        fontSize = 12.sp,
                        color = funkyColors.mute,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        if (userErrorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = funkyColors.danger.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, funkyColors.danger.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SoftWarningIcon(color = funkyColors.danger, size = 20.dp)
                    Text(
                        text = userErrorMessage!!,
                        fontSize = 13.sp,
                        color = funkyColors.danger,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { userErrorMessage = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        SoftCloseIcon(
                            color = funkyColors.danger,
                            size = 14.dp
                        )
                    }
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
                .background(funkyColors.surface)
                .border(1.dp, funkyColors.line, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Google Drive Storage", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = funkyColors.ink)
                        val statusText = when {
                            isReconnectNeeded -> "Reconnect needed"
                            !driveAccountName.isNullOrBlank() -> "Connected: $driveAccountName"
                            else -> "Not connected"
                        }
                        val statusColor = when {
                            isReconnectNeeded -> funkyColors.danger
                            !driveAccountName.isNullOrBlank() -> funkyColors.ok
                            else -> funkyColors.mute
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFFFFD000), funkyColors.mango))
                            )
                            .clickable { initiateDriveConnection() },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isReconnectNeeded) "Reconnect Google Drive" else "Connect Google Drive",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = funkyColors.onMango
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            SoftArrowRightIcon(color = funkyColors.onMango, size = 16.dp)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showChangePasswordDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, funkyColors.line),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = funkyColors.ink),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Change password", fontSize = 11.sp, maxLines = 1, color = funkyColors.ink)
                        }

                        OutlinedButton(
                            onClick = {
                                driveAccountName = null
                                ChatBackupPreferencesManager.setDriveAccountName(context, null)
                                Toast.makeText(context, "Google Drive disconnected.", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, funkyColors.danger.copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Disconnect", fontSize = 11.sp, color = funkyColors.danger, maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Delete backup from Google Drive", fontSize = 12.sp, color = funkyColors.danger)
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
                .background(funkyColors.surface)
                .border(1.dp, funkyColors.line, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).background(funkyColors.soft, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            BackupClockGraphic(
                                color = if (isDark) funkyColors.mango else Color(0xFF854D0E),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Last Backup", fontSize = 11.sp, color = funkyColors.mute)
                            Text(formattedLastBackupTime, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = funkyColors.ink)
                        }
                    }

                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).background(funkyColors.soft, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            BackupInfoGraphic(
                                color = if (isDark) funkyColors.mango else Color(0xFF854D0E),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Backup Size", fontSize = 11.sp, color = funkyColors.mute)
                            Text(formattedBackupSize, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = funkyColors.ink)
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
                                color = funkyColors.ink,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "$backupProgressPercent%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = funkyColors.mango
                            )
                        }

                        LinearProgressIndicator(
                            progress = { backupProgressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = funkyColors.mango,
                            trackColor = funkyColors.soft
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedButton(
                            onClick = {
                                backupCancellationRequested = true
                                backupProgressText = "Cancelling..."
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, funkyColors.danger.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel Backup", color = funkyColors.danger, fontSize = 13.sp)
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFFFFD000), funkyColors.mango))
                            )
                            .clickable {
                                if (driveAccountName.isNullOrBlank()) {
                                    initiateDriveConnection()
                                } else {
                                    onBackUpNowClicked()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Back up now", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = funkyColors.onMango)
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
                .background(funkyColors.surface)
                .border(1.dp, funkyColors.line, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column {
                Text("Backup Preferences", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = funkyColors.ink)
                Text("Customize how your chats are backed up.", fontSize = 12.sp, color = funkyColors.mute)

                Spacer(modifier = Modifier.height(14.dp))

                Text("Auto Backup Frequency", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = funkyColors.ink)
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
                                .background(if (isSelected) funkyColors.mango else funkyColors.bg)
                                .border(1.dp, if (isSelected) funkyColors.mango else funkyColors.line, RoundedCornerShape(19.dp))
                                .clickable {
                                    autoBackupFreq = freq
                                    val prefFreq = freq.uppercase()
                                    ChatBackupPreferencesManager.setAutoBackupFrequency(context, prefFreq)
                                    ChatBackupScheduler.updateBackupSchedule(context)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                freq,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) funkyColors.onMango else funkyColors.ink
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Include Photos & Media", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = funkyColors.ink)
                        Text("Back up images, videos, voice notes and other media files.", fontSize = 11.sp, color = funkyColors.mute)
                    }
                    Switch(
                        checked = includePhotos,
                        onCheckedChange = {
                            includePhotos = it
                            ChatBackupPreferencesManager.setIncludeMedia(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = funkyColors.surface,
                            checkedTrackColor = funkyColors.mango,
                            uncheckedThumbColor = funkyColors.mute,
                            uncheckedTrackColor = funkyColors.bg
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Back up over Wi-Fi only", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = funkyColors.ink)
                        Text("Avoid using cellular data for backups.", fontSize = 11.sp, color = funkyColors.mute)
                    }
                    Switch(
                        checked = wifiOnly,
                        onCheckedChange = {
                            wifiOnly = it
                            ChatBackupPreferencesManager.setWifiOnly(context, it)
                            ChatBackupScheduler.updateBackupSchedule(context)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = funkyColors.surface,
                            checkedTrackColor = funkyColors.mango,
                            uncheckedThumbColor = funkyColors.mute,
                            uncheckedTrackColor = funkyColors.bg
                        )
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
                .background(funkyColors.soft)
                .border(1.dp, funkyColors.line, RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FunkyLightbulbIcon(
                    color = funkyColors.mango,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tip", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = funkyColors.ink)
                    Text(
                        "Regular backups keep your conversations safe, even if you change or lose your device.",
                        fontSize = 11.sp,
                        color = funkyColors.mute,
                        lineHeight = 15.sp
                    )
                }
                Soft3DCloudIcon(size = 48.dp)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }

    // Dialog: Protect your backup (First-Time Setup)
    if (showProtectBackupDialog) {
        val hasVaultConfigured = remember { VaultSecurityManager.isVaultConfigured(context) }

        if (hasVaultConfigured) {
            var enteredPassword by remember { mutableStateOf("") }
            var isPasswordVisible by remember { mutableStateOf(false) }
            var isVerifying by remember { mutableStateOf(false) }
            var dialogError by remember { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = { if (!isVerifying) showProtectBackupDialog = false },
                containerColor = funkyColors.surface,
                titleContentColor = funkyColors.ink,
                textContentColor = funkyColors.ink,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(funkyColors.soft, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            SoftWarningIcon(
                                color = funkyColors.mango,
                                size = 20.dp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Protect your backup", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = funkyColors.ink)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Enter your vault password to initialize and encrypt your cloud backup.",
                            fontSize = 13.sp,
                            color = funkyColors.mute,
                            lineHeight = 18.sp
                        )

                        OutlinedTextField(
                            value = enteredPassword,
                            onValueChange = {
                                enteredPassword = it
                                dialogError = null
                            },
                            label = { Text("Enter your vault password", color = funkyColors.mute) },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    SoftEyeIcon(
                                        visible = isPasswordVisible,
                                        tint = funkyColors.mute,
                                        size = 20.dp
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = funkyColors.mango,
                                unfocusedBorderColor = funkyColors.line,
                                focusedTextColor = funkyColors.ink,
                                unfocusedTextColor = funkyColors.ink
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            isError = dialogError != null,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (dialogError != null) {
                            Text(
                                text = dialogError!!,
                                color = funkyColors.danger,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (enteredPassword.isBlank()) {
                                dialogError = "Please enter your vault password."
                                return@Button
                            }
                            coroutineScope.launch {
                                isVerifying = true
                                dialogError = null
                                val unlockResult = VaultSecurityManager.unlockWithPassword(context, enteredPassword)
                                if (unlockResult.isFailure) {
                                    isVerifying = false
                                    dialogError = "Wrong password"
                                    return@launch
                                }
                                try {
                                    DriveBackupEngine.initializeBackupKeyHeader(
                                        context = context,
                                        accountEmail = driveAccountName ?: "",
                                        vaultPassword = enteredPassword
                                    )
                                    showProtectBackupDialog = false
                                    userErrorMessage = null
                                    triggerBackupNow()
                                } catch (e: Exception) {
                                    val msg = e.message.orEmpty()
                                    dialogError = if (msg.contains("password", ignoreCase = true)) {
                                        "Wrong password"
                                    } else {
                                        "Could not initialize backup. Please check your internet connection."
                                    }
                                } finally {
                                    isVerifying = false
                                }
                            }
                        },
                        enabled = !isVerifying,
                        colors = ButtonDefaults.buttonColors(containerColor = funkyColors.mango)
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = funkyColors.onMango)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Continue", color = funkyColors.onMango, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showProtectBackupDialog = false },
                        enabled = !isVerifying
                    ) {
                        Text("Cancel", color = funkyColors.mute)
                    }
                }
            )
        } else {
            var newPassword by remember { mutableStateOf("") }
            var confirmPassword by remember { mutableStateOf("") }
            var isNewPasswordVisible by remember { mutableStateOf(false) }
            var isConfirmPasswordVisible by remember { mutableStateOf(false) }
            var understandChecked by remember { mutableStateOf(false) }
            var isSubmitting by remember { mutableStateOf(false) }
            var dialogError by remember { mutableStateOf<String?>(null) }

            val (strengthLabel, strengthColor) = remember(newPassword) {
                calculatePasswordStrength(newPassword)
            }

            AlertDialog(
                onDismissRequest = { if (!isSubmitting) showProtectBackupDialog = false },
                containerColor = funkyColors.surface,
                titleContentColor = funkyColors.ink,
                textContentColor = funkyColors.ink,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(funkyColors.soft, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            SoftWarningIcon(
                                color = funkyColors.mango,
                                size = 20.dp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Protect your backup", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = funkyColors.ink)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Create a password to protect your end-to-end encrypted backup.",
                            fontSize = 13.sp,
                            color = funkyColors.mute,
                            lineHeight = 18.sp
                        )

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                dialogError = null
                            },
                            label = { Text("Create a backup password", color = funkyColors.mute) },
                            visualTransformation = if (isNewPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isNewPasswordVisible = !isNewPasswordVisible }) {
                                    SoftEyeIcon(
                                        visible = isNewPasswordVisible,
                                        tint = funkyColors.mute,
                                        size = 20.dp
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = funkyColors.mango,
                                unfocusedBorderColor = funkyColors.line,
                                focusedTextColor = funkyColors.ink,
                                unfocusedTextColor = funkyColors.ink
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            isError = dialogError != null,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (newPassword.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Password strength:", fontSize = 12.sp, color = funkyColors.mute)
                                Text(
                                    text = strengthLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = strengthColor
                                )
                            }
                        }

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                dialogError = null
                            },
                            label = { Text("Confirm password", color = funkyColors.mute) },
                            visualTransformation = if (isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isConfirmPasswordVisible = !isConfirmPasswordVisible }) {
                                    SoftEyeIcon(
                                        visible = isConfirmPasswordVisible,
                                        tint = funkyColors.mute,
                                        size = 20.dp
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = funkyColors.mango,
                                unfocusedBorderColor = funkyColors.line,
                                focusedTextColor = funkyColors.ink,
                                unfocusedTextColor = funkyColors.ink
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            isError = dialogError != null,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { understandChecked = !understandChecked }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = understandChecked,
                                onCheckedChange = { understandChecked = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = funkyColors.mango,
                                    checkmarkColor = funkyColors.onMango
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "I understand that if I forget this password my backup cannot be recovered",
                                fontSize = 12.sp,
                                color = funkyColors.ink,
                                lineHeight = 16.sp
                            )
                        }

                        if (dialogError != null) {
                            Text(
                                text = dialogError!!,
                                color = funkyColors.danger,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                confirmButton = {
                    val canSubmit = newPassword.length >= 8 &&
                            newPassword == confirmPassword &&
                            understandChecked &&
                            !isSubmitting

                    Button(
                        onClick = {
                            if (newPassword.length < 8) {
                                dialogError = "Password must be at least 8 characters."
                                return@Button
                            }
                            if (newPassword != confirmPassword) {
                                dialogError = "Passwords do not match."
                                return@Button
                            }
                            if (!understandChecked) {
                                dialogError = "Please check the confirmation box."
                                return@Button
                            }

                            coroutineScope.launch {
                                isSubmitting = true
                                dialogError = null
                                try {
                                    val setupResult = VaultSecurityManager.setupVaultWithPassword(context, newPassword)
                                    if (setupResult.isFailure) {
                                        dialogError = "Could not create backup password. Please try again."
                                        return@launch
                                    }
                                    DriveBackupEngine.initializeBackupKeyHeader(
                                        context = context,
                                        accountEmail = driveAccountName ?: "",
                                        vaultPassword = newPassword
                                    )
                                    showProtectBackupDialog = false
                                    userErrorMessage = null
                                    triggerBackupNow()
                                } catch (e: Exception) {
                                    dialogError = "Could not set up backup. Please check your internet connection."
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        },
                        enabled = canSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = funkyColors.mango)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = funkyColors.onMango)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Create backup", color = funkyColors.onMango, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showProtectBackupDialog = false },
                        enabled = !isSubmitting
                    ) {
                        Text("Cancel", color = funkyColors.mute)
                    }
                }
            )
        }
    }

    // Dialog: Change Backup Password (Re-wrap only)
    if (showChangePasswordDialog) {
        var currentPasswordInput by remember { mutableStateOf("") }
        var newPasswordInput by remember { mutableStateOf("") }
        var isSubmitting by remember { mutableStateOf(false) }
        var dialogError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showChangePasswordDialog = false },
            containerColor = funkyColors.surface,
            titleContentColor = funkyColors.ink,
            textContentColor = funkyColors.ink,
            title = { Text("Change Backup Password", fontWeight = FontWeight.Bold, color = funkyColors.ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "This will re-encrypt your backup master key with a new vault password without re-uploading your chat data.",
                        fontSize = 12.sp,
                        color = funkyColors.mute
                    )

                    OutlinedTextField(
                        value = currentPasswordInput,
                        onValueChange = { currentPasswordInput = it },
                        label = { Text("Current Vault Password", color = funkyColors.mute) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = funkyColors.mango,
                            unfocusedBorderColor = funkyColors.line,
                            focusedTextColor = funkyColors.ink,
                            unfocusedTextColor = funkyColors.ink
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("New Vault Password", color = funkyColors.mute) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = funkyColors.mango,
                            unfocusedBorderColor = funkyColors.line,
                            focusedTextColor = funkyColors.ink,
                            unfocusedTextColor = funkyColors.ink
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (dialogError != null) {
                        Text(dialogError!!, color = funkyColors.danger, fontSize = 12.sp)
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
                    colors = ButtonDefaults.buttonColors(containerColor = funkyColors.mango)
                ) {
                    Text("Update Password", color = funkyColors.onMango, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showChangePasswordDialog = false },
                    enabled = !isSubmitting
                ) {
                    Text("Cancel", color = funkyColors.mute)
                }
            }
        )
    }

    // Dialog: Delete Backup From Google Drive
    if (showDeleteConfirmDialog) {
        var isDeleting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isDeleting) showDeleteConfirmDialog = false },
            containerColor = funkyColors.surface,
            titleContentColor = funkyColors.ink,
            textContentColor = funkyColors.ink,
            title = { Text("Delete Backup from Drive?", fontWeight = FontWeight.Bold, color = funkyColors.ink) },
            text = {
                Text(
                    "Are you sure you want to delete your backup from Google Drive? This will permanently delete all encrypted chat backups and media from Google Drive. This action cannot be undone.",
                    fontSize = 13.sp,
                    color = funkyColors.mute,
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
                    colors = ButtonDefaults.buttonColors(containerColor = funkyColors.danger)
                ) {
                    Text("Delete Backup", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false },
                    enabled = !isDeleting
                ) {
                    Text("Cancel", color = funkyColors.mute)
                }
            }
        )
    }
}

@Composable
private fun BackupClockGraphic(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val sw = size.minDimension * 0.10f
        val r = size.minDimension * 0.38f
        val cx = size.width / 2f
        val cy = size.height / 2f
        drawCircle(color = color, radius = r, center = Offset(cx, cy), style = Stroke(width = sw))
        drawLine(color, Offset(cx, cy), Offset(cx, cy - r * 0.52f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color, Offset(cx, cy), Offset(cx + r * 0.45f, cy + r * 0.15f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
private fun BackupInfoGraphic(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val sw = size.minDimension * 0.10f
        val r = size.minDimension * 0.38f
        val cx = size.width / 2f
        val cy = size.height / 2f
        drawCircle(color = color, radius = r, center = Offset(cx, cy), style = Stroke(width = sw))
        drawCircle(color = color, radius = sw * 0.65f, center = Offset(cx, cy - r * 0.38f))
        drawLine(color, Offset(cx, cy - r * 0.05f), Offset(cx, cy + r * 0.45f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

private fun calculatePasswordStrength(password: String): Pair<String, Color> {
    if (password.length < 8) {
        return Pair("Too short (min 8 chars)", Color(0xFFDC2626))
    }
    var score = 0
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isLowerCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    if (password.length >= 12) score++

    return when {
        score <= 2 -> Pair("Weak", Color(0xFFEF4444))
        score == 3 -> Pair("Fair", Color(0xFFF59E0B))
        score == 4 -> Pair("Good", Color(0xFF3B82F6))
        else -> Pair("Strong", Color(0xFF10B981))
    }
}
