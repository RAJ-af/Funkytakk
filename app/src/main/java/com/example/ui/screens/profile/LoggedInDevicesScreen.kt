package com.example.ui.screens.profile

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.UserDeviceRow
import com.example.repository.DeviceSessionRepository
import com.example.ui.components.SecureWindow
import com.example.ui.theme.LocalFunkyColors
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.OffsetDateTime
import kotlin.math.abs

@Composable
fun LoggedInDevicesScreen(
    onBack: () -> Unit,
    onSignedOutCurrentDevice: () -> Unit
) {
    SecureWindow(enabled = true)
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val funkyColors = LocalFunkyColors.current

    var devices by remember { mutableStateOf<List<UserDeviceRow>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isMutating by remember { mutableStateOf(false) }

    var pendingRevokeDevice by remember { mutableStateOf<UserDeviceRow?>(null) }
    var showRevokeOthersConfirm by remember { mutableStateOf(false) }

    fun loadDevices() {
        scope.launch {
            isLoading = true
            errorText = null
            val res = DeviceSessionRepository.getMyDevices(context)
            res.onSuccess { rows ->
                devices = rows.sortedWith(
                    compareByDescending<UserDeviceRow> { it.is_current }
                        .thenBy { it.revoked }
                )
            }.onFailure { err ->
                errorText = err.message ?: "Unable to load devices."
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadDevices()
    }

    val hasOtherActiveDevices = remember(devices) {
        devices.any { !it.is_current && !it.revoked }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(funkyColors.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(funkyColors.soft)
                        .clickable { onBack() }
                        .testTag("logged_in_devices_back_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_back),
                        contentDescription = "Back",
                        tint = funkyColors.ink,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Logged-in devices",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = funkyColors.ink
                    )
                    Text(
                        text = "Active & recent sessions",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = funkyColors.mute
                    )
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(funkyColors.soft)
                        .clickable(enabled = !isLoading && !isMutating) { loadDevices() }
                        .testTag("logged_in_devices_refresh_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh devices",
                        tint = funkyColors.ink,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            if (isLoading && devices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = funkyColors.mango)
                }
            } else if (!errorText.isNullOrBlank() && devices.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = errorText!!,
                        color = funkyColors.danger,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { loadDevices() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = funkyColors.mango,
                            contentColor = funkyColors.onMango
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("logged_in_devices_retry_button")
                    ) {
                        Text("Retry", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (devices.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(funkyColors.soft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Devices,
                            contentDescription = null,
                            tint = funkyColors.mute,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No devices yet.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = funkyColors.ink,
                        modifier = Modifier.testTag("logged_in_devices_empty_text")
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (hasOtherActiveDevices) {
                        item(key = "logout_other_banner") {
                            OutlinedButton(
                                onClick = { showRevokeOthersConfirm = true },
                                enabled = !isMutating,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, funkyColors.danger.copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("logout_all_other_devices_button")
                            ) {
                                Text(
                                    text = "Log out all other devices",
                                    color = funkyColors.danger,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    items(devices, key = { it.id }) { device ->
                        DeviceRowCard(
                            device = device,
                            isMutating = isMutating,
                            onLogoutDeviceClick = { pendingRevokeDevice = device }
                        )
                    }
                }
            }
        }
    }

    pendingRevokeDevice?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!isMutating) pendingRevokeDevice = null },
            containerColor = funkyColors.surface,
            titleContentColor = funkyColors.ink,
            textContentColor = funkyColors.ink,
            title = {
                Text(
                    text = "Log out this device?",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    text = if (target.is_current) {
                        "You are logging out ${target.device_name} (this device). You will be returned to the login screen."
                    } else {
                        "Log out ${target.device_name}? This device will be signed out."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val row = target
                        pendingRevokeDevice = null
                        isMutating = true
                        scope.launch {
                            val res = DeviceSessionRepository.revokeDevice(context, row)
                            isMutating = false
                            res.onSuccess {
                                if (row.is_current) {
                                    onSignedOutCurrentDevice()
                                } else {
                                    Toast.makeText(context, "Device signed out", Toast.LENGTH_SHORT).show()
                                    loadDevices()
                                }
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Failed to log out device", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = funkyColors.danger,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_logout_single_device_button")
                ) {
                    Text("Log out", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRevokeDevice = null }) {
                    Text("Cancel", color = funkyColors.mute)
                }
            }
        )
    }

    if (showRevokeOthersConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isMutating) showRevokeOthersConfirm = false },
            containerColor = funkyColors.surface,
            titleContentColor = funkyColors.ink,
            textContentColor = funkyColors.ink,
            title = {
                Text(
                    text = "Log out all other devices?",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    text = "All other signed-in devices will be signed out. Only this device will stay signed in."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRevokeOthersConfirm = false
                        isMutating = true
                        scope.launch {
                            val res = DeviceSessionRepository.revokeOtherDevices(context)
                            isMutating = false
                            res.onSuccess {
                                Toast.makeText(context, "Other devices signed out", Toast.LENGTH_SHORT).show()
                                loadDevices()
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Failed to log out other devices", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = funkyColors.danger,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_logout_other_devices_button")
                ) {
                    Text("Log out all other", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevokeOthersConfirm = false }) {
                    Text("Cancel", color = funkyColors.mute)
                }
            }
        )
    }
}

@Composable
private fun DeviceRowCard(
    device: UserDeviceRow,
    isMutating: Boolean,
    onLogoutDeviceClick: () -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    val lastActiveLabel = remember(device.last_seen_at, device.last_login_at) {
        formatRelativeLastActive(device.last_seen_at ?: device.last_login_at)
    }
    val rowAlpha = if (device.revoked) 0.55f else 1f

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = funkyColors.surface,
        border = BorderStroke(
            width = if (device.is_current && !device.revoked) 1.5.dp else 1.dp,
            color = if (device.is_current && !device.revoked) funkyColors.mango else funkyColors.line
        ),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
            .testTag("device_row_${device.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (device.revoked) funkyColors.soft else funkyColors.mango.copy(alpha = 0.16f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhoneAndroid,
                        contentDescription = null,
                        tint = if (device.revoked) funkyColors.mute else funkyColors.mango,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = device.device_name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (device.is_current && !device.revoked) {
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = funkyColors.mango.copy(alpha = 0.20f)
                            ) {
                                Text(
                                    text = "This device",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = funkyColors.ink,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        if (device.revoked) {
                            Surface(
                                shape = RoundedCornerShape(99.dp),
                                color = funkyColors.soft
                            ) {
                                Text(
                                    text = "Signed out",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = funkyColors.mute,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    val osAndVersion = listOfNotNull(
                        device.os.takeIf { it.isNotBlank() },
                        device.app_version.takeIf { it.isNotBlank() }?.let { "v$it" }
                    ).joinToString(" · ")
                    if (osAndVersion.isNotBlank()) {
                        Text(
                            text = osAndVersion,
                            fontSize = 12.5.sp,
                            color = funkyColors.mute,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = lastActiveLabel,
                        fontSize = 12.sp,
                        color = funkyColors.mute
                    )
                }
            }

            if (!device.revoked) {
                OutlinedButton(
                    onClick = onLogoutDeviceClick,
                    enabled = !isMutating,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, funkyColors.line),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("logout_device_button_${device.id}")
                ) {
                    Text(
                        text = "Log out this device",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = funkyColors.danger
                    )
                }
            }
        }
    }
}

internal fun formatRelativeLastActive(isoTimestamp: String?): String {
    if (isoTimestamp.isNullOrBlank()) return "Last active recently"
    val instantMs = parseIsoTimestampMillis(isoTimestamp) ?: return "Last active recently"
    val diffMs = abs(System.currentTimeMillis() - instantMs)
    val seconds = diffMs / 1000L
    val relative = when {
        seconds < 60L -> "just now"
        seconds < 3600L -> {
            val mins = seconds / 60L
            "$mins min ago"
        }
        seconds < 86_400L -> {
            val hrs = seconds / 3600L
            if (hrs == 1L) "1 hour ago" else "$hrs hours ago"
        }
        seconds < 30L * 86_400L -> {
            val days = seconds / 86_400L
            if (days == 1L) "1 day ago" else "$days days ago"
        }
        else -> {
            val months = seconds / (30L * 86_400L)
            if (months == 1L) "1 month ago" else "$months months ago"
        }
    }
    return "Last active $relative"
}

private fun parseIsoTimestampMillis(raw: String): Long? {
    val clean = raw.trim()
    return try {
        Instant.parse(clean).toEpochMilli()
    } catch (_: Exception) {
        try {
            OffsetDateTime.parse(clean).toInstant().toEpochMilli()
        } catch (_: Exception) {
            null
        }
    }
}
