package com.example.ui.screens.chat

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.SupabaseGroupMember
import com.example.SupabaseSessionManager
import com.example.db.LocalGroupConversation
import com.example.db.LocalGroupMessage
import com.example.repository.GroupChatRepository
import com.example.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    group: LocalGroupConversation,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onNavigateToManageMembers: () -> Unit,
    onNavigateToGroupSettings: () -> Unit,
    onAddMemberClick: () -> Unit,
    onLeaveGroupClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val myUserId = remember { SupabaseSessionManager.getUserId(context) ?: "" }

    val canEditInfo = isAdmin || group.whoCanEditInfo.equals("members", ignoreCase = true)
    val canAddMembers = isAdmin || group.whoCanAddMembers.equals("members", ignoreCase = true)

    var membersList by remember { mutableStateOf<List<SupabaseGroupMember>>(emptyList()) }
    var isLoadingMembers by remember { mutableStateOf(true) }
    var isUploadingAvatar by remember { mutableStateOf(false) }

    var showInviteSheet by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showEditInfoDialog by remember { mutableStateOf(false) }
    var showLocalSearchSheet by remember { mutableStateOf(false) }
    var showAllMediaSheet by remember { mutableStateOf(false) }
    var isLeaving by remember { mutableStateOf(false) }

    val sharedMediaFlow = remember(group.id) { GroupChatRepository.getSharedMediaFlow(group.id) }
    val sharedMediaMessages by sharedMediaFlow.collectAsState(initial = emptyList())

    val isMuted = remember(group.mutedUntil) {
        (group.mutedUntil ?: 0L) > System.currentTimeMillis()
    }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && canEditInfo) {
            scope.launch {
                isUploadingAvatar = true
                val (bytes, mime) = withContext(Dispatchers.IO) {
                    val b = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    val m = context.contentResolver.getType(uri) ?: "image/jpeg"
                    b to m
                }
                if (bytes != null && bytes.isNotEmpty()) {
                    val res = GroupChatRepository.uploadAndSetGroupAvatar(context, group.id, bytes, mime)
                    isUploadingAvatar = false
                    res.onSuccess {
                        Toast.makeText(context, "Group photo updated", Toast.LENGTH_SHORT).show()
                    }.onFailure { err ->
                        Toast.makeText(context, err.message ?: "Failed to update group photo", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    isUploadingAvatar = false
                }
            }
        }
    }

    fun loadOverviewAndMembers() {
        scope.launch {
            GroupChatRepository.getGroupOverview(context, group.id)
            isLoadingMembers = true
            val res = GroupChatRepository.getGroupMembersResult(context, group.id)
            isLoadingMembers = false
            res.onSuccess { list ->
                membersList = list
            }
        }
    }

    LaunchedEffect(group.id) {
        loadOverviewAndMembers()
    }

    LaunchedEffect(group.id) {
        GroupChatRepository.liveGroupUpdates.collect { updatedId ->
            if (updatedId == group.id) {
                loadOverviewAndMembers()
            }
        }
    }

    BackHandler { onBack() }

    val joinedMembers = remember(membersList) {
        membersList.filter { it.status.equals("joined", ignoreCase = true) }
    }
    val onlineCount = remember(joinedMembers) {
        joinedMembers.count { it.presence_status?.equals("online", ignoreCase = true) == true }
    }
    val isMemberListRestricted = !isAdmin &&
        (group.memberListVisibility.equals("admins", ignoreCase = true) || (!isLoadingMembers && joinedMembers.isEmpty()))

    Scaffold(
        containerColor = Color(0xFFFAFAF9),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    GroupBackIcon(size = 20.dp, color = Color(0xFF0F172A))
                }

                Text(
                    text = "Group Info",
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                if (canEditInfo) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .clickable { showEditInfoDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        GroupEditPencilIcon(size = 18.dp, color = Color(0xFF0F172A))
                    }
                } else {
                    Spacer(modifier = Modifier.size(42.dp))
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Hero Group Avatar + Title + Joined Count
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .clickable(enabled = canEditInfo && !isUploadingAvatar) {
                            avatarPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7))
                            .border(2.5.dp, Color(0xFFFFC529), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploadingAvatar) {
                            CircularProgressIndicator(
                                color = Color(0xFFB45309),
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(30.dp)
                            )
                        } else if (group.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = group.avatarUrl,
                                contentDescription = group.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            GroupUsersIcon(size = 40.dp, color = Color(0xFFB45309))
                        }
                    }

                    if (canEditInfo) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(Color(0xFFFFC529))
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            GroupCameraIcon(size = 14.dp, color = Color.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = group.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                val memberLabel = if (group.memberCount == 1) "1 member" else "${group.memberCount} members"
                val subHeader = if (onlineCount > 0) "$memberLabel · $onlineCount online" else memberLabel
                Text(
                    text = subHeader,
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }

            // 2. Quick Action 4-Buttons Row (Mute, Search, Share, Settings)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoQuickActionCard(
                    modifier = Modifier.weight(1f),
                    icon = {
                        GroupBellIcon(
                            size = 20.dp,
                            color = if (isMuted) Color(0xFFD97706) else Color(0xFF0F172A)
                        )
                    },
                    label = if (isMuted) "Muted" else "Mute",
                    onClick = {
                        scope.launch {
                            val res = if (isMuted) {
                                GroupChatRepository.updateMyGroupPrefs(
                                    context = context,
                                    groupId = group.id,
                                    clearMute = true
                                )
                            } else {
                                GroupChatRepository.updateMyGroupPrefs(
                                    context = context,
                                    groupId = group.id,
                                    mutedUntilIso = "2099-12-31T23:59:59Z",
                                    clearMute = false
                                )
                            }
                            res.onSuccess {
                                Toast.makeText(
                                    context,
                                    if (isMuted) "Notifications unmuted" else "Notifications muted",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Failed to update mute", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
                InfoQuickActionCard(
                    modifier = Modifier.weight(1f),
                    icon = { GroupSearchIcon(size = 20.dp, color = Color(0xFF0F172A)) },
                    label = "Search",
                    onClick = { showLocalSearchSheet = true }
                )
                InfoQuickActionCard(
                    modifier = Modifier.weight(1f),
                    icon = { GroupShareIcon(size = 20.dp, color = Color(0xFF0F172A)) },
                    label = "Invite",
                    onClick = { showInviteSheet = true }
                )
                InfoQuickActionCard(
                    modifier = Modifier.weight(1f),
                    icon = { GroupGearIcon(size = 20.dp, color = Color(0xFF0F172A)) },
                    label = "Settings",
                    onClick = onNavigateToGroupSettings
                )
            }

            // 3. About Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = canEditInfo) { showEditInfoDialog = true },
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(3.5.dp)
                                    .height(13.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFFF59E0B))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "About",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                        if (canEditInfo) {
                            Text(
                                text = "Edit",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = group.description.ifBlank { "No group description set." },
                        fontSize = 13.5.sp,
                        color = if (group.description.isBlank()) Color(0xFF94A3B8) else Color(0xFF475569),
                        lineHeight = 19.sp
                    )
                }
            }

            // 4. Quick Settings Card (Invite Link, Disappearing Messages, Join Requests)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    InfoRowItem(
                        icon = { GroupLinkIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                        title = "Invite link",
                        value = "Share / Manage",
                        onClick = { showInviteSheet = true }
                    )
                    HorizontalDivider(color = Color(0xFFF8FAFC))
                    InfoRowItem(
                        icon = { GroupClockIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                        title = "Disappearing messages",
                        value = GroupChatRepository.formatDisappearingLabel(group.disappearingSeconds),
                        onClick = onNavigateToGroupSettings
                    )
                    if (isAdmin && group.pendingRequestsCount > 0) {
                        HorizontalDivider(color = Color(0xFFF8FAFC))
                        InfoRowItem(
                            icon = { GroupUsersIcon(size = 18.dp, color = Color(0xFFD97706)) },
                            title = "Pending join requests",
                            value = "${group.pendingRequestsCount} waiting",
                            onClick = onNavigateToManageMembers
                        )
                    }
                }
            }

            // 5. Members Preview Card (using real joined_count and get_group_members)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(3.5.dp)
                                    .height(13.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFFF59E0B))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Members (${group.memberCount})",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        if (canAddMembers) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onAddMemberClick() }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "+ Add",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isLoadingMembers) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFFFC529),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else if (isMemberListRestricted) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF8FAFC))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GroupLockIcon(size = 18.dp, color = Color(0xFFD97706))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Member list is visible to admins only",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                        }
                    } else {
                        val previewMembers = joinedMembers.take(5)
                        previewMembers.forEachIndexed { index, member ->
                            val isSelf = member.user_id == myUserId
                            val isMemberAdmin = member.role.equals("admin", ignoreCase = true)
                            val baseName = member.profile_name?.takeIf { it.isNotBlank() }
                                ?: member.username?.takeIf { it.isNotBlank() }
                                ?: "Member (${member.user_id.take(6)})"
                            val displayName = if (isSelf) "$baseName (You)" else baseName
                            val isOnline = member.presence_status?.equals("online", ignoreCase = true) == true
                            val subline = when {
                                !member.username.isNullOrBlank() -> "@${member.username}"
                                isOnline -> "Online"
                                else -> "Joined"
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(modifier = Modifier.size(42.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFEF3C7)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!member.avatar_url.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = member.avatar_url,
                                                    contentDescription = displayName,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape)
                                                )
                                            } else {
                                                Text(
                                                    text = baseName.take(1).uppercase(),
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB45309)
                                                )
                                            }
                                        }
                                        if (isOnline) {
                                            Box(
                                                modifier = Modifier
                                                    .size(11.dp)
                                                    .align(Alignment.BottomEnd)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF22C55E))
                                                    .border(1.8.dp, Color.White, CircleShape)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = displayName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = subline,
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF94A3B8),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (isMemberAdmin) {
                                    Surface(
                                        shape = RoundedCornerShape(50.dp),
                                        color = Color(0xFFFEF3C7)
                                    ) {
                                        Text(
                                            text = "Admin",
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309)
                                        )
                                    }
                                }
                            }

                            if (index < previewMembers.lastIndex) {
                                HorizontalDivider(color = Color(0xFFF8FAFC))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onNavigateToManageMembers() },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Manage all members (${group.memberCount}) →",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            }

            // 6. Real Shared Media Card (from Local E2EE DB only!)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .width(3.5.dp)
                                    .height(13.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFFF59E0B))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Shared media (${sharedMediaMessages.size})",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        if (sharedMediaMessages.isNotEmpty()) {
                            Text(
                                text = "See all",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706),
                                modifier = Modifier.clickable { showAllMediaSheet = true }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (sharedMediaMessages.isEmpty()) {
                        Text(
                            text = "No shared photos or voice messages saved on this device yet.",
                            fontSize = 12.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sharedMediaMessages.take(4).forEach { mediaMsg ->
                                val localFile = mediaMsg.localMediaPath?.let { File(it) }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFFEF3C7))
                                        .clickable { showAllMediaSheet = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (mediaMsg.messageType == "image" && localFile != null && localFile.exists()) {
                                        AsyncImage(
                                            model = localFile,
                                            contentDescription = "Shared photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else if (mediaMsg.messageType == "voice") {
                                        GroupMicIcon(size = 22.dp, color = Color(0xFFB45309))
                                    } else {
                                        GroupImagePlaceholderIcon(size = 22.dp, color = Color(0xFFB45309))
                                    }
                                }
                            }
                            repeat((4 - sharedMediaMessages.take(4).size).coerceAtLeast(0)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // 7. Leave Group Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { showLeaveDialog = true },
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, Color(0xFFFECACA))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 15.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GroupLeaveDoorIcon(size = 18.dp, color = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Leave group",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showInviteSheet) {
        GroupInviteBottomSheet(
            group = group,
            onDismiss = { showInviteSheet = false }
        )
    }

    if (showEditInfoDialog) {
        var draftName by remember(group.name) { mutableStateOf(group.name) }
        var draftDesc by remember(group.description) { mutableStateOf(group.description) }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showEditInfoDialog = false },
            title = { Text("Edit Group Info", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = draftName,
                        onValueChange = { if (it.length <= 60) draftName = it },
                        label = { Text("Group name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draftDesc,
                        onValueChange = { if (it.length <= 300) draftDesc = it },
                        label = { Text("Description") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isSaving && draftName.trim().isNotEmpty(),
                    onClick = {
                        val patch = mutableMapOf<String, Any?>()
                        if (draftName.trim() != group.name) patch["name"] = draftName.trim()
                        if (draftDesc.trim() != group.description) patch["description"] = draftDesc.trim()
                        if (patch.isEmpty()) {
                            showEditInfoDialog = false
                            return@TextButton
                        }
                        scope.launch {
                            isSaving = true
                            val res = GroupChatRepository.updateGroupSettings(context, group.id, patch)
                            isSaving = false
                            res.onSuccess {
                                showEditInfoDialog = false
                                Toast.makeText(context, "Group info updated", Toast.LENGTH_SHORT).show()
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Failed to update group info", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditInfoDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showLocalSearchSheet) {
        LocalGroupSearchBottomSheet(
            groupId = group.id,
            onDismiss = { showLocalSearchSheet = false }
        )
    }

    if (showAllMediaSheet) {
        LocalGroupMediaBottomSheet(
            groupId = group.id,
            onDismiss = { showAllMediaSheet = false }
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { if (!isLeaving) showLeaveDialog = false },
            title = { Text("Leave Group?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to leave \"${group.name}\"?") },
            confirmButton = {
                TextButton(
                    enabled = !isLeaving,
                    onClick = {
                        scope.launch {
                            isLeaving = true
                            val res = GroupChatRepository.leaveGroup(context, group.id)
                            isLeaving = false
                            res.onSuccess {
                                showLeaveDialog = false
                                Toast.makeText(context, "Left group", Toast.LENGTH_SHORT).show()
                                onLeaveGroupClick()
                            }.onFailure { err ->
                                Toast.makeText(context, err.message ?: "Failed to leave group", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Leave", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalGroupSearchBottomSheet(
    groupId: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<LocalGroupMessage>>(emptyList()) }
    val timeFormatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    LaunchedEffect(groupId, query) {
        results = if (query.isBlank()) {
            emptyList()
        } else {
            GroupChatRepository.searchMessagesLocally(groupId, query)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Search in Conversation",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "Searches end-to-end encrypted messages stored on this device.",
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Type a word or phrase...") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (query.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Enter text above to search local messages", color = Color(0xFF94A3B8), fontSize = 13.5.sp)
                }
            } else if (results.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No messages matched \"$query\"", color = Color(0xFF64748B), fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results, key = { it.id }) { msg ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (msg.isOutgoing) "You" else msg.senderName,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309)
                                    )
                                    Text(
                                        text = timeFormatter.format(Date(msg.timestamp)),
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = msg.text,
                                    fontSize = 13.5.sp,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalGroupMediaBottomSheet(
    groupId: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val allFlow = remember(groupId) { GroupChatRepository.getAllMediaAndLinksFlow(groupId) }
    val allMessages by allFlow.collectAsState(initial = emptyList())

    var selectedTab by remember { mutableStateOf("Media") }

    val mediaItems = remember(allMessages) {
        allMessages.filter { it.messageType == "image" || it.messageType == "voice" }
    }
    val linkItems = remember(allMessages) {
        allMessages.filter {
            it.messageType == "text" && (it.text.contains("http://", ignoreCase = true) || it.text.contains("https://", ignoreCase = true) || it.text.contains("funkytalk://", ignoreCase = true))
        }
    }
    val timeFormatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.78f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Media & Links",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "Stored locally in your end-to-end encrypted vault.",
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Media" to "Media (${mediaItems.size})", "Links" to "Links (${linkItems.size})").forEach { (key, label) ->
                    val isSelected = selectedTab == key
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .clickable { selectedTab = key },
                        shape = RoundedCornerShape(50.dp),
                        color = if (isSelected) Color(0xFFFFC529) else Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == "Media") {
                if (mediaItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No photos or voice messages saved yet", color = Color(0xFF94A3B8), fontSize = 13.5.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(mediaItems, key = { it.id }) { item ->
                            val localFile = item.localMediaPath?.let { File(it) }
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFFFEF3C7)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (item.messageType == "image" && localFile != null && localFile.exists()) {
                                            AsyncImage(
                                                model = localFile,
                                                contentDescription = "Photo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else if (item.messageType == "voice") {
                                            GroupMicIcon(size = 22.dp, color = Color(0xFFB45309))
                                        } else {
                                            GroupImagePlaceholderIcon(size = 22.dp, color = Color(0xFFB45309))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (item.messageType == "voice") "Voice message" else "Encrypted Photo",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "From ${if (item.isOutgoing) "You" else item.senderName} · ${timeFormatter.format(Date(item.timestamp))}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                if (linkItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No links shared in this conversation yet", color = Color(0xFF94A3B8), fontSize = 13.5.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(linkItems, key = { it.id }) { msg ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = msg.text,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFB45309)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${if (msg.isOutgoing) "You" else msg.senderName} · ${timeFormatter.format(Date(msg.timestamp))}",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoQuickActionCard(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon()
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun InfoRowItem(
    icon: @Composable () -> Unit,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF0F172A)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontSize = 12.5.sp,
                color = Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.width(6.dp))
            GroupChevronRightIcon(size = 14.dp, color = Color(0xFF94A3B8))
        }
    }
}
