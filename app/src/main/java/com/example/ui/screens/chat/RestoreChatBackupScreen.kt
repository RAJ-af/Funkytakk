package com.example.ui.screens.chat

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.BackupCrypto
import com.example.crypto.DriveBackupEngine
import com.example.crypto.DriveBackupInfo
import com.example.crypto.VaultSecurityManager
import com.example.service.ChatRestoreWorker
import com.example.service.RestoreProgressTracker
import com.example.ui.components.GroupLockIcon
import com.example.ui.components.Soft3DCloudIcon
import com.example.ui.components.Soft3DShieldIcon
import com.example.ui.theme.LocalFunkyColors
import com.example.util.ChatBackupPreferencesManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RestoreChatBackupScreen(
    onRestoreSuccess: () -> Unit,
    onSkipRestore: () -> Unit
) {
    com.example.ui.components.SecureWindow(enabled = true)
    val context = LocalContext.current
    val funkyColors = LocalFunkyColors.current
    val coroutineScope = rememberCoroutineScope()
    val firebaseUser = FirebaseAuth.getInstance().currentUser

    var googleAccountName by remember {
        mutableStateOf(ChatBackupPreferencesManager.getDriveAccountName(context))
    }
    var isCheckingDrive by remember { mutableStateOf(false) }
    var driveBackupInfo by remember { mutableStateOf<DriveBackupInfo?>(null) }
    var noBackupFound by remember { mutableStateOf(false) }

    var vaultPasswordInput by remember { mutableStateOf("") }
    var isVerifyingPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var failedAttemptsCount by remember { mutableIntStateOf(0) }
    var lockoutSecondsRemaining by remember { mutableIntStateOf(0) }

    val restoreState by RestoreProgressTracker.progress.collectAsState()

    BackHandler(enabled = !restoreState.isRestoring && !isVerifyingPassword) {
        onSkipRestore()
    }

    // Countdown timer for lockout
    LaunchedEffect(lockoutSecondsRemaining) {
        if (lockoutSecondsRemaining > 0) {
            delay(1000L)
            lockoutSecondsRemaining--
        }
    }

    // Check Drive whenever googleAccountName is present
    fun checkDriveForBackup(accountEmail: String) {
        coroutineScope.launch {
            isCheckingDrive = true
            errorMessage = null
            noBackupFound = false
            driveBackupInfo = null
            try {
                val info = DriveBackupEngine.checkBackupOnDrive(context, accountEmail)
                if (info != null && info.hasBackup) {
                    driveBackupInfo = info
                    noBackupFound = false
                } else {
                    driveBackupInfo = null
                    noBackupFound = true
                }
            } catch (e: Exception) {
                errorMessage = "Could not check Google Drive: ${e.message}"
            } finally {
                isCheckingDrive = false
            }
        }
    }

    LaunchedEffect(googleAccountName) {
        val email = googleAccountName
        if (!email.isNullOrBlank() && driveBackupInfo == null && !noBackupFound && !restoreState.isRestoring) {
            checkDriveForBackup(email)
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
                    googleAccountName = account.email
                    ChatBackupPreferencesManager.setDriveAccountName(context, account.email)
                    Toast.makeText(context, "Connected: ${account.email}", Toast.LENGTH_SHORT).show()
                    checkDriveForBackup(account.email!!)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to connect Google Drive: ${e.message}", Toast.LENGTH_LONG).show()
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

    fun startRestore() {
        if (lockoutSecondsRemaining > 0) return
        if (vaultPasswordInput.isBlank()) {
            errorMessage = "Please enter your vault password."
            return
        }
        val currentUid = firebaseUser?.uid
        if (currentUid.isNullOrBlank()) {
            errorMessage = "User not authenticated."
            return
        }
        val headerJson = driveBackupInfo?.headerJson
        if (headerJson.isNullOrBlank()) {
            errorMessage = "Backup header missing on Google Drive."
            return
        }

        coroutineScope.launch {
            isVerifyingPassword = true
            errorMessage = null

            try {
                // Verify account UID & unwrap master key BK
                val masterKey = try {
                    BackupCrypto.unwrapMasterKey(headerJson, vaultPasswordInput, currentUid)
                } catch (e: IllegalArgumentException) {
                    if (e.message?.contains("another FunkyTalk account", ignoreCase = true) == true) {
                        throw IllegalArgumentException("This backup belongs to another FunkyTalk account")
                    }
                    throw IllegalArgumentException("Wrong password")
                } catch (e: javax.crypto.AEADBadTagException) {
                    throw IllegalArgumentException("Wrong password")
                } catch (e: Exception) {
                    val msg = e.message.orEmpty()
                    if (msg.contains("another FunkyTalk account", ignoreCase = true)) {
                        throw IllegalArgumentException("This backup belongs to another FunkyTalk account")
                    }
                    throw IllegalArgumentException("Wrong password")
                }

                // Password verified! Save master key and configure vault
                BackupCrypto.saveMasterKeyToKeystore(context, masterKey)
                if (!VaultSecurityManager.isVaultConfigured(context)) {
                    VaultSecurityManager.setupVaultWithPassword(context, vaultPasswordInput)
                } else {
                    VaultSecurityManager.unlockWithPassword(context, vaultPasswordInput)
                }
                ChatBackupPreferencesManager.setCachedVaultPassword(context, vaultPasswordInput)

                // Enqueue background restore worker via WorkManager (foreground service type dataSync)
                ChatRestoreWorker.enqueueRestore(context, vaultPasswordInput)
            } catch (e: IllegalArgumentException) {
                if (e.message == "This backup belongs to another FunkyTalk account") {
                    errorMessage = "This backup belongs to another FunkyTalk account"
                } else {
                    failedAttemptsCount++
                    val lockout = when (failedAttemptsCount) {
                        1, 2 -> 0
                        3 -> 3
                        4 -> 10
                        else -> 30
                    }
                    lockoutSecondsRemaining = lockout
                    errorMessage = if (lockout > 0) {
                        "Wrong password. Please wait $lockout seconds."
                    } else {
                        "Wrong password"
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Verification error: ${e.message}"
            } finally {
                isVerifyingPassword = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(funkyColors.bg)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Cloud Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(funkyColors.soft),
                contentAlignment = Alignment.Center
            ) {
                Soft3DCloudIcon(size = 64.dp)
            }

            Text(
                text = "Restore your chats",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = funkyColors.ink
            )

            Text(
                text = "Restore your encrypted messages, media attachments and group history safely from your Google Drive.",
                fontSize = 13.sp,
                color = funkyColors.mute,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            // Warning Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = funkyColors.soft,
                border = BorderStroke(1.dp, funkyColors.mango.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Soft3DShieldIcon(size = 28.dp)
                    Text(
                        text = "If you forget your vault password, your backup cannot be recovered.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (funkyColors.isDark) funkyColors.mango else Color(0xFFB45309),
                        lineHeight = 16.sp
                    )
                }
            }

            // Case 1: Active Restore in Progress
            if (restoreState.isRestoring) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = funkyColors.surface,
                    border = BorderStroke(1.dp, funkyColors.line),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Restoring Chats (${restoreState.percent}%)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.ink
                        )

                        LinearProgressIndicator(
                            progress = { restoreState.percent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = funkyColors.mango,
                            trackColor = funkyColors.soft
                        )

                        Text(
                            text = restoreState.statusText,
                            fontSize = 12.sp,
                            color = funkyColors.mute
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "You can leave this screen and start using FunkyTalk while restoration finishes in the background.",
                            fontSize = 12.sp,
                            color = funkyColors.mute,
                            lineHeight = 16.sp
                        )

                        Button(
                            onClick = onRestoreSuccess,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = funkyColors.mango,
                                contentColor = funkyColors.onMango
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Continue to FunkyTalk", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (restoreState.isComplete) {
                // Restore Completed Successfully
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = funkyColors.surface,
                    border = BorderStroke(1.dp, funkyColors.line),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Chats restored successfully!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.ok
                        )

                        Button(
                            onClick = onRestoreSuccess,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = funkyColors.mango,
                                contentColor = funkyColors.onMango
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open FunkyTalk", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (googleAccountName.isNullOrBlank()) {
                // Step 1: Connect Google Drive
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = funkyColors.surface,
                    border = BorderStroke(1.dp, funkyColors.line),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Connect Google Drive",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.ink
                        )

                        Button(
                            onClick = { initiateDriveConnection() },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = funkyColors.mango,
                                contentColor = funkyColors.onMango
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Connect Google Drive", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                // Skip for now button
                OutlinedButton(
                    onClick = {
                        ChatBackupPreferencesManager.setHasSkippedRestore(context, true)
                        onSkipRestore()
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, funkyColors.line),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip for now", color = funkyColors.mute)
                }
            } else if (isCheckingDrive) {
                // Checking Drive
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = funkyColors.surface,
                    border = BorderStroke(1.dp, funkyColors.line),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = funkyColors.mango)
                        Text("Searching for backups on Google Drive...", fontSize = 13.sp, color = funkyColors.mute)
                    }
                }
            } else if (noBackupFound) {
                // No backup found
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = funkyColors.surface,
                    border = BorderStroke(1.dp, funkyColors.line),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No backup found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.ink
                        )

                        Text(
                            text = "No encrypted chat backup was found in your Google Drive account ($googleAccountName).",
                            fontSize = 12.sp,
                            color = funkyColors.mute,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                ChatBackupPreferencesManager.setHasSkippedRestore(context, true)
                                onSkipRestore()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = funkyColors.mango,
                                contentColor = funkyColors.onMango
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Continue", fontWeight = FontWeight.Bold)
                        }

                        TextButton(onClick = { initiateDriveConnection() }) {
                            Text("Connect a different Google account", fontSize = 12.sp, color = funkyColors.mute)
                        }
                    }
                }
            } else if (driveBackupInfo != null) {
                // Backup found!
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = funkyColors.surface,
                    border = BorderStroke(1.dp, funkyColors.line),
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
                                    text = "Backup found: ${driveBackupInfo!!.dateString}, ${driveBackupInfo!!.sizeString}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = funkyColors.ink
                                )
                                Text(
                                    text = "Account: $googleAccountName",
                                    fontSize = 12.sp,
                                    color = funkyColors.mute
                                )
                            }
                        }

                        HorizontalDivider(color = funkyColors.line)

                        OutlinedTextField(
                            value = vaultPasswordInput,
                            onValueChange = {
                                vaultPasswordInput = it
                                errorMessage = null
                            },
                            enabled = !isVerifyingPassword && lockoutSecondsRemaining == 0,
                            label = { Text("Enter your vault password", color = funkyColors.mute) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            leadingIcon = { GroupLockIcon(size = 20.dp, color = funkyColors.mute) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = funkyColors.ink,
                                unfocusedTextColor = funkyColors.ink,
                                focusedBorderColor = funkyColors.mango,
                                unfocusedBorderColor = funkyColors.line,
                                cursorColor = funkyColors.mango
                            ),
                            isError = errorMessage != null
                        )

                        if (!errorMessage.isNullOrBlank()) {
                            Text(
                                text = errorMessage.orEmpty(),
                                fontSize = 12.sp,
                                color = funkyColors.danger,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (lockoutSecondsRemaining > 0) {
                            Text(
                                text = "Too many failed attempts. Try again in ${lockoutSecondsRemaining}s",
                                fontSize = 12.sp,
                                color = funkyColors.danger,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (isVerifyingPassword) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = funkyColors.mango)
                                Text("Verifying password...", fontSize = 12.sp, color = funkyColors.mute)
                            }
                        }

                        Button(
                            onClick = { startRestore() },
                            enabled = !isVerifyingPassword && lockoutSecondsRemaining == 0 && vaultPasswordInput.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = funkyColors.mango,
                                contentColor = funkyColors.onMango
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Restore", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                ChatBackupPreferencesManager.setHasSkippedRestore(context, true)
                                onSkipRestore()
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, funkyColors.line),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Skip for now", color = funkyColors.mute)
                        }
                    }
                }
            }
        }
    }
}
