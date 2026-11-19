package com.example.ui.screens.profile

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.SupportChatManager
import com.example.TelemetryManager
import com.example.gifting.CuteCoinIcon
import com.example.repository.FeedbackRepository
import com.example.repository.InboxDrainTracker
import com.example.service.PushNotificationManager
import com.example.ui.components.Soft3DCloudIcon
import com.example.ui.components.SoftBlockIcon
import com.example.ui.components.SoftHeadsetIcon
import com.example.ui.components.SoftShieldIcon
import com.example.ui.components.SoftStarIcon
import com.example.ui.screens.ProfileVerifiedBadge
import com.example.ui.theme.AppThemeManager
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.LocalFunkyColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    walletBalance: Int,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenCoinStore: () -> Unit,
    onOpenDecorationStore: () -> Unit,
    onOpenHelpSupport: () -> Unit = {},
    onOpenFeedbackHub: () -> Unit = {},
    onOpenSupportChat: () -> Unit,
    onOpenModerationHub: () -> Unit,
    onOpenBlockedUsers: () -> Unit,
    onOpenBlockedGroups: () -> Unit,
    onOpenChatBackup: () -> Unit,
    onOpenChatAppearance: () -> Unit,
    onOpenLoggedInDevices: () -> Unit = {},
    onOpenDebugGallery: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val funkyColors = LocalFunkyColors.current
    val isDark = LocalAppDarkTheme.current
    val pushDebugState by PushNotificationManager.debugState.collectAsState()
    val usageDataEnabled by TelemetryManager.isUsageDataEnabled.collectAsState()
    val supportUnread by SupportChatManager.unreadCount.collectAsState()
    val feedbackUpdates by FeedbackRepository.myUpdatesCount.collectAsState()
    val helpTotalBadge = supportUnread + feedbackUpdates
    val appFlags by com.example.AppFlagsRepository.flags.collectAsState()

    BackHandler { onBack() }

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
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(funkyColors.soft)
                        .clickable { onBack() }
                        .testTag("settings_back_button"),
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
                        text = "Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = funkyColors.ink
                    )
                    Text(
                        text = "Account, theme & preferences",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = funkyColors.mute
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(funkyColors.soft)
                        .clickable { AppThemeManager.toggleDarkTheme(context) }
                        .testTag("settings_top_theme_toggle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = if (isDark) R.drawable.ic_sun else R.drawable.ic_moon),
                        contentDescription = if (isDark) "Switch to Light Theme" else "Switch to Dark Theme",
                        tint = if (isDark) Color(0xFFFBBF24) else Color(0xFF7C3AED),
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ---------------------------------------------------------
                // 1. APPEARANCE THEME SWITCHER
                // ---------------------------------------------------------
                SettingsSectionHeader(title = "Appearance", accentColor = funkyColors.mango)

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = funkyColors.surface,
                    border = BorderStroke(1.dp, funkyColors.line),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(Color(0xFFFFB627).copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_palette),
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "App Color Theme",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = funkyColors.ink
                                )
                                Text(
                                    text = if (isDark) "Warm Brownish Dark + Mango" else "Cream White + Mango (Default)",
                                    fontSize = 12.sp,
                                    color = funkyColors.mute
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ThemeChoiceCard(
                                title = "Cream Light",
                                subtitle = "Cream & Mango",
                                previewBg = Color(0xFFFFF8E7),
                                previewAccent = Color(0xFFFFB627),
                                iconRes = R.drawable.ic_sun,
                                iconTint = Color(0xFFF59E0B),
                                selected = !isDark,
                                onClick = { AppThemeManager.setDarkTheme(context, false) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_theme_light")
                            )
                            ThemeChoiceCard(
                                title = "Cocoa Dark",
                                subtitle = "Brownish & Mango",
                                previewBg = Color(0xFF16120D),
                                previewAccent = Color(0xFFFFB627),
                                iconRes = R.drawable.ic_moon,
                                iconTint = Color(0xFF8B5CF6),
                                selected = isDark,
                                onClick = { AppThemeManager.setDarkTheme(context, true) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_theme_dark")
                            )
                        }
                    }
                }

                // ---------------------------------------------------------
                // 2. STORE & WALLET
                // ---------------------------------------------------------
                SettingsSectionHeader(title = "Store & Wallet", accentColor = Color(0xFFF59E0B))

                SettingsGroupCard {
                    SettingsRowItem(
                        title = "Decoration Store",
                        subtitle = "Avatar frames, entry rockets, chat bubbles & themes",
                        iconBg = Color(0xFFFFF3C4),
                        iconContent = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_palette),
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = onOpenDecorationStore
                    )
                    if (appFlags.coinStoreEnabled) {
                        HorizontalDivider(
                            color = funkyColors.line,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        SettingsRowItem(
                            title = "Coin Store",
                            subtitle = "Top up coins for gifts & cosmetics",
                            trailingBadge = "$walletBalance",
                            trailingBadgeBg = funkyColors.mango,
                            trailingBadgeFg = funkyColors.onMango,
                            iconBg = Color(0xFFFEF3C7),
                            iconContent = {
                                CuteCoinIcon(modifier = Modifier.size(22.dp))
                            },
                            onClick = onOpenCoinStore
                        )
                    }
                }

                // ---------------------------------------------------------
                // 3. ACCOUNT & PRIVACY
                // ---------------------------------------------------------
                SettingsSectionHeader(title = "Account & Privacy", accentColor = Color(0xFF3B82F6))

                SettingsGroupCard {
                    SettingsRowItem(
                        title = "Edit Profile",
                        subtitle = "Display name, avatar, banner, languages & interests",
                        iconBg = Color(0xFFE0F2FE),
                        iconContent = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_profile),
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = onEditProfile
                    )
                    HorizontalDivider(color = funkyColors.line, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRowItem(
                        title = "Logged-in devices",
                        subtitle = "Manage devices signed into your account",
                        iconBg = Color(0xFFE0F2FE),
                        iconContent = {
                            Icon(
                                imageVector = Icons.Outlined.Devices,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = onOpenLoggedInDevices
                    )
                    HorizontalDivider(color = funkyColors.line, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRowItem(
                        title = "Chat Backup & Recovery",
                        subtitle = "Encrypted Google Drive chat backup",
                        iconBg = Color(0xFFE0E7FF),
                        iconContent = {
                            Soft3DCloudIcon(size = 22.dp)
                        },
                        onClick = onOpenChatBackup
                    )
                    HorizontalDivider(color = funkyColors.line, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRowItem(
                        title = "Chat Appearance & AI",
                        subtitle = "Butter, Sky, Mint, Graphite presets · Text size · Cloud AI",
                        iconBg = Color(0xFFEDE9FE),
                        iconContent = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_sparkle),
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = onOpenChatAppearance
                    )
                    HorizontalDivider(color = funkyColors.line, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRowItem(
                        title = "Blocked Users",
                        subtitle = "Manage blocked accounts",
                        iconBg = Color(0xFFFFEDD5),
                        iconContent = {
                            SoftBlockIcon(size = 20.dp, color = Color(0xFFEA580C))
                        },
                        onClick = onOpenBlockedUsers
                    )
                    HorizontalDivider(color = funkyColors.line, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRowItem(
                        title = "Blocked Groups",
                        subtitle = "View and unblock group conversations",
                        iconBg = Color(0xFFFEF3C7),
                        iconContent = {
                            SoftBlockIcon(size = 20.dp, color = Color(0xFFB45309))
                        },
                        onClick = onOpenBlockedGroups
                    )
                    HorizontalDivider(color = funkyColors.line, modifier = Modifier.padding(horizontal = 16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { TelemetryManager.setUsageDataEnabled(context, !usageDataEnabled) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFDCFCE7)),
                            contentAlignment = Alignment.Center
                        ) {
                            SoftShieldIcon(size = 20.dp, color = Color(0xFF16A34A))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Share anonymous usage data",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = funkyColors.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Helps us fix crashes and improve FunkyTalk",
                                fontSize = 11.5.sp,
                                color = funkyColors.mute,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Switch(
                            checked = usageDataEnabled,
                            onCheckedChange = { TelemetryManager.setUsageDataEnabled(context, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = funkyColors.onMango,
                                checkedTrackColor = funkyColors.mango,
                                uncheckedThumbColor = funkyColors.mute,
                                uncheckedTrackColor = funkyColors.soft
                            ),
                            modifier = Modifier.testTag("settings_usage_data_switch")
                        )
                    }
                }

                // ---------------------------------------------------------
                // 4. SAFETY & SUPPORT
                // ---------------------------------------------------------
                SettingsSectionHeader(title = "Safety & Support", accentColor = Color(0xFF10B981))

                SettingsGroupCard {
                    if (isAdmin) {
                        SettingsRowItem(
                            title = "Safety & Moderation Hub",
                            subtitle = "Admin review dashboard",
                            iconBg = Color(0xFFD1FAE5),
                            iconContent = {
                                ProfileVerifiedBadge(modifier = Modifier.size(21.dp))
                            },
                            onClick = onOpenModerationHub
                        )
                        HorizontalDivider(color = funkyColors.line, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    SettingsRowItem(
                        title = "Help & Support",
                        subtitle = "24/7 support chat, bug reports & feature requests",
                        trailingBadge = if (helpTotalBadge > 0) "$helpTotalBadge" else null,
                        trailingBadgeBg = Color(0xFFEF4444),
                        trailingBadgeFg = Color.White,
                        iconBg = Color(0xFFDBEAFE),
                        iconContent = {
                            SoftHeadsetIcon(size = 20.dp, color = Color(0xFF2563EB))
                        },
                        onClick = onOpenHelpSupport
                    )
                    HorizontalDivider(color = funkyColors.line, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRowItem(
                        title = "Feedback & Feature Requests",
                        subtitle = "Vote on upcoming features & track your reports",
                        trailingBadge = if (feedbackUpdates > 0) "$feedbackUpdates" else null,
                        trailingBadgeBg = Color(0xFFEF4444),
                        trailingBadgeFg = Color.White,
                        iconBg = Color(0xFFFEF3C7),
                        iconContent = {
                            SoftStarIcon(size = 20.dp, color = Color(0xFFD97706))
                        },
                        onClick = onOpenFeedbackHub
                    )
                    HorizontalDivider(color = funkyColors.line, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRowItem(
                        title = "24/7 Official Support Chat",
                        subtitle = "Direct conversation with our support team",
                        trailingBadge = if (supportUnread > 0) "$supportUnread" else null,
                        trailingBadgeBg = Color(0xFFEF4444),
                        trailingBadgeFg = Color.White,
                        iconBg = Color(0xFFE0F2FE),
                        iconContent = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_chats),
                                contentDescription = null,
                                tint = Color(0xFF0EA5E9),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = onOpenSupportChat
                    )
                }

                // ---------------------------------------------------------
                // 5. ABOUT & DIAGNOSTICS
                // ---------------------------------------------------------
                SettingsSectionHeader(title = "About", accentColor = funkyColors.mute)

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = funkyColors.surface,
                    border = BorderStroke(1.dp, funkyColors.line),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (BuildConfig.DEBUG) {
                                    Modifier.combinedClickable(
                                        onClick = {},
                                        onLongClick = onOpenDebugGallery
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(funkyColors.soft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_verified),
                                    contentDescription = null,
                                    tint = Color(0xFF0EA5E9),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "FunkyTalk v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = funkyColors.ink
                                )
                                Text(
                                    text = "Animated emoji by Google, licensed CC BY 4.0",
                                    fontSize = 11.5.sp,
                                    color = funkyColors.mute
                                )
                            }
                        }

                        if (BuildConfig.DEBUG) {
                            val permAndChannelLine = remember(pushDebugState) {
                                PushNotificationManager.getPermissionAndChannelDebugLine(context)
                            }
                            val inboxStats by InboxDrainTracker.stats.collectAsState()
                            val formattedDrainTime = remember(inboxStats.lastDrainTime) {
                                if (inboxStats.lastDrainTime > 0) {
                                    SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                                        .format(Date(inboxStats.lastDrainTime))
                                } else {
                                    "Never"
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 6.dp),
                                color = funkyColors.line
                            )
                            Text(
                                text = "Inbox: fetched ${inboxStats.fetchedCount}, saved ${inboxStats.savedCount}, failed ${inboxStats.failedCount}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = funkyColors.ink,
                                modifier = Modifier.testTag("inbox_drain_debug_stats")
                            )
                            Text(
                                text = "Last drain: $formattedDrainTime",
                                fontSize = 11.sp,
                                color = funkyColors.mute,
                                modifier = Modifier.testTag("inbox_drain_debug_time")
                            )
                            if (!inboxStats.lastFailureReason.isNullOrBlank()) {
                                Text(
                                    text = "Last failure: ${inboxStats.lastFailureReason}",
                                    fontSize = 11.sp,
                                    color = funkyColors.danger,
                                    modifier = Modifier.testTag("inbox_drain_debug_error")
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 6.dp),
                                color = funkyColors.line
                            )
                            Text(
                                text = pushDebugState.summaryLine,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (pushDebugState.isRegistered) funkyColors.ok else funkyColors.danger,
                                modifier = Modifier.testTag("push_debug_status_text")
                            )
                            Text(
                                text = pushDebugState.httpStatusLine,
                                fontSize = 11.sp,
                                color = if (!pushDebugState.lastError.isNullOrBlank() && !pushDebugState.isRegistered) {
                                    funkyColors.danger
                                } else {
                                    funkyColors.mute
                                },
                                modifier = Modifier.testTag("push_debug_http_status_text")
                            )
                            Text(
                                text = permAndChannelLine,
                                fontSize = 11.sp,
                                color = funkyColors.mute,
                                modifier = Modifier.testTag("push_debug_perm_channel_text")
                            )
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val ok = PushNotificationManager.forceReregisterPush(context)
                                        Toast.makeText(
                                            context,
                                            if (ok) "Push token re-registered!" else "Re-register push failed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                enabled = !pushDebugState.isReRegistering,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, funkyColors.line),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .testTag("reregister_push_button")
                            ) {
                                if (pushDebugState.isReRegistering) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = funkyColors.mango
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Re-registering…",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = funkyColors.ink
                                    )
                                } else {
                                    Text(
                                        text = "Re-register push",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = funkyColors.ink
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    TelemetryManager.triggerDebugNonFatal()
                                    Toast.makeText(
                                        context,
                                        "Debug non-fatal logged (collection disabled in debug builds)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, funkyColors.line),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                                    .testTag("debug_test_crashlytics_button")
                            ) {
                                Text(
                                    text = "Test Crashlytics non-fatal",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = funkyColors.ink
                                )
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = funkyColors.line
                            )
                            Text(
                                text = "Release Flag Overrides (Debug Only)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = funkyColors.ink
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Coin Store enabled", fontSize = 12.sp, color = funkyColors.ink)
                                Switch(
                                    checked = appFlags.coinStoreEnabled,
                                    onCheckedChange = { com.example.AppFlagsRepository.setDebugCoinStoreOverride(it) },
                                    modifier = Modifier.testTag("debug_flag_coin_store_switch")
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("VIP enabled", fontSize = 12.sp, color = funkyColors.ink)
                                Switch(
                                    checked = appFlags.vipEnabled,
                                    onCheckedChange = { com.example.AppFlagsRepository.setDebugVipOverride(it) },
                                    modifier = Modifier.testTag("debug_flag_vip_switch")
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Force Update Gate", fontSize = 12.sp, color = funkyColors.ink)
                                Switch(
                                    checked = appFlags.minSupportedVersionCode > BuildConfig.VERSION_CODE,
                                    onCheckedChange = { enabled ->
                                        com.example.AppFlagsRepository.setDebugMinVersionOverride(
                                            if (enabled) BuildConfig.VERSION_CODE + 1 else 0
                                        )
                                    },
                                    modifier = Modifier.testTag("debug_flag_update_gate_switch")
                                )
                            }
                            if (com.example.AppFlagsRepository.hasDebugOverrides()) {
                                TextButton(
                                    onClick = { com.example.AppFlagsRepository.clearDebugOverrides() },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Reset to server flags", fontSize = 11.5.sp, color = funkyColors.mango)
                                }
                            }
                        }
                    }
                }

                // ---------------------------------------------------------
                // 6. SESSION & DANGER ZONE
                // ---------------------------------------------------------
                SettingsSectionHeader(title = "Account Actions", accentColor = funkyColors.danger)

                SettingsGroupCard {
                    SettingsRowItem(
                        title = "Log Out of Account",
                        subtitle = "Sign out from this device",
                        titleColor = Color(0xFFE11D48),
                        iconBg = Color(0xFFFFE4E6),
                        iconContent = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_logout),
                                contentDescription = null,
                                tint = Color(0xFFE11D48),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = onLogout
                    )
                    HorizontalDivider(color = funkyColors.line, modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsRowItem(
                        title = "Delete Account",
                        subtitle = "Permanently erase profile, chats and data",
                        titleColor = funkyColors.danger,
                        iconBg = Color(0xFFFEE2E2),
                        iconContent = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_alert),
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = onDeleteAccount
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    accentColor: Color
) {
    val funkyColors = LocalFunkyColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(15.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = funkyColors.mute
        )
    }
}

@Composable
private fun SettingsGroupCard(
    content: @Composable ColumnScope.() -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = funkyColors.surface,
        border = BorderStroke(1.dp, funkyColors.line),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun SettingsRowItem(
    title: String,
    subtitle: String,
    iconBg: Color,
    iconContent: @Composable () -> Unit,
    onClick: () -> Unit,
    titleColor: Color? = null,
    trailingBadge: String? = null,
    trailingBadgeBg: Color = Color.Transparent,
    trailingBadgeFg: Color = Color.Unspecified
) {
    val funkyColors = LocalFunkyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            iconContent()
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor ?: funkyColors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = funkyColors.mute,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (!trailingBadge.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = trailingBadgeBg
            ) {
                Text(
                    text = trailingBadge,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = trailingBadgeFg,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = funkyColors.mute.copy(alpha = 0.7f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ThemeChoiceCard(
    title: String,
    subtitle: String,
    previewBg: Color,
    previewAccent: Color,
    @DrawableRes iconRes: Int,
    iconTint: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val funkyColors = LocalFunkyColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) funkyColors.soft else funkyColors.bg,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) funkyColors.mango else funkyColors.line
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(previewBg)
                    .border(2.dp, previewAccent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = funkyColors.ink,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = funkyColors.mute,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (selected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(funkyColors.mango),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check),
                        contentDescription = "Selected",
                        tint = funkyColors.onMango,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
