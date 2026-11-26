package com.example.ui.screens.moments

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.MomentAuthorDto
import com.example.MomentDto
import com.example.crypto.VaultSecurityManager
import com.example.crypto.VaultStatus
import com.example.data.SavedMomentsStore
import com.example.repository.MomentsRepository
import com.example.ui.components.MomentsV2FeedViewerDialog
import com.example.ui.components.PresenceAvatar
import com.example.ui.screens.chat.VaultManagementDialogs
import com.example.ui.theme.LocalFunkyColors

@Composable
fun SavedMomentsScreen(
    currentUserNativeLanguage: String = "",
    onBack: () -> Unit,
    onOpenMomentAuthorProfile: (MomentAuthorDto) -> Unit = {}
) {
    val context = LocalContext.current
    val funkyColors = LocalFunkyColors.current
    val vaultStatus by VaultSecurityManager.vaultStatus.collectAsState()
    val savedMoments by SavedMomentsStore.savedMoments.collectAsState()

    var showVaultDialog by remember { mutableStateOf(false) }
    var activeMomentId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(vaultStatus) {
        if (vaultStatus == VaultStatus.UNLOCKED) {
            SavedMomentsStore.loadIfUnlocked(context)
            showVaultDialog = false
        }
    }

    BackHandler {
        if (activeMomentId != null) {
            activeMomentId = null
        } else {
            onBack()
        }
    }

    Scaffold(
        containerColor = funkyColors.bg,
        topBar = {
            Surface(
                color = funkyColors.surface,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("saved_moments_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = funkyColors.ink
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Saved Moments",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = funkyColors.ink
                        )
                        Text(
                            text = "Encrypted on this device",
                            fontSize = 12.sp,
                            color = funkyColors.mute
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Device-local E2EE disclosure banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = funkyColors.mango.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, funkyColors.mango.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = funkyColors.mango,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Saved moments are stored encrypted only on this phone using your Vault key. Uninstalling the app or losing your Vault password will lock or erase them.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = funkyColors.ink
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (VaultSecurityManager.isVaultLocked() || vaultStatus != VaultStatus.UNLOCKED) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = funkyColors.surface,
                        border = BorderStroke(1.dp, funkyColors.line),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 420.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .background(funkyColors.mango.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = funkyColors.mango,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = if (vaultStatus == VaultStatus.NOT_CONFIGURED) "Set Up Vault to Save Moments" else "Vault Locked",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = funkyColors.ink
                            )
                            Text(
                                text = "Unlock your encrypted Vault to view or manage your saved moments on this device.",
                                fontSize = 13.sp,
                                color = funkyColors.mute
                            )
                            Button(
                                onClick = { showVaultDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = funkyColors.mango,
                                    contentColor = funkyColors.onMango
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("saved_moments_unlock_button")
                            ) {
                                Text(
                                    text = if (vaultStatus == VaultStatus.NOT_CONFIGURED) "Set Vault Password" else "Unlock Vault",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else if (savedMoments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(funkyColors.soft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                tint = funkyColors.mango,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Text(
                            text = "No saved moments yet",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.ink
                        )
                        Text(
                            text = "Tap the bookmark icon on any moment in the feed to keep an encrypted copy here.",
                            fontSize = 13.sp,
                            color = funkyColors.mute
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(savedMoments, key = { it.id }) { moment ->
                        SavedMomentCard(
                            moment = moment,
                            onClick = { activeMomentId = moment.id },
                            onAuthorClick = {
                                if (moment.author.id.isNotBlank()) {
                                    onOpenMomentAuthorProfile(moment.author)
                                }
                            },
                            onRemove = {
                                SavedMomentsStore.removeMoment(context, moment.id)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showVaultDialog) {
        VaultManagementDialogs(
            onDismissRequest = { showVaultDialog = false }
        )
    }

    activeMomentId?.let { targetId ->
        if (savedMoments.isNotEmpty()) {
            MomentsV2FeedViewerDialog(
                moments = savedMoments,
                initialMomentId = targetId,
                currentUserNativeLanguage = currentUserNativeLanguage,
                onDismiss = { activeMomentId = null },
                onAuthorClick = { author ->
                    activeMomentId = null
                    onOpenMomentAuthorProfile(author)
                }
            )
        }
    }
}

@Composable
private fun SavedMomentCard(
    moment: MomentDto,
    onClick: () -> Unit,
    onAuthorClick: () -> Unit,
    onRemove: () -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    val authorName = moment.author.name?.takeIf { it.isNotBlank() }
        ?: moment.author.username?.takeIf { it.isNotBlank() }
        ?: "Learner"
    val firstMedia = moment.media.firstOrNull()
    val thumbUrl = firstMedia?.thumb_url?.takeIf { it.isNotBlank() } ?: firstMedia?.url?.takeIf { it.isNotBlank() }
    val preset = remember(moment.bg_key) { MomentsRepository.resolveBgPreset(moment.bg_key) }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = funkyColors.surface,
        border = BorderStroke(1.dp, funkyColors.line),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_moment_card_${moment.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onAuthorClick),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PresenceAvatar(
                        avatarUrl = moment.author.avatar_url,
                        size = 38.dp,
                        userId = moment.author.id,
                        countryCode = moment.author.country_code,
                        showFlagBadge = !moment.author.country_code.isNullOrBlank(),
                        enableFrameAnimation = false
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = authorName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = moment.language,
                            fontSize = 11.5.sp,
                            color = funkyColors.mute,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("saved_moment_remove_${moment.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Remove saved moment",
                        tint = funkyColors.danger
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!moment.title.isNullOrBlank()) {
                        Text(
                            text = moment.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.ink,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (!moment.content.isNullOrBlank()) {
                        Text(
                            text = moment.content,
                            fontSize = 13.sp,
                            color = funkyColors.mute,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (thumbUrl != null) {
                    AsyncImage(
                        model = thumbUrl,
                        contentDescription = moment.title ?: "Saved moment preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(preset.colors)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = moment.language.take(2).uppercase(),
                            color = if (preset.isDarkText) Color(0xFF1B1608) else Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}
