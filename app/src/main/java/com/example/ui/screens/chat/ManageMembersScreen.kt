package com.example.ui.screens.chat

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.SupabaseClient
import com.example.SupabaseGroupMember
import com.example.SupabaseSessionManager
import com.example.db.LocalGroupConversation
import com.example.repository.EncryptedChatRepository
import com.example.repository.GroupChatRepository
import com.example.ui.chat.*
import com.example.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageMembersScreen(
    group: LocalGroupConversation,
    isAdmin: Boolean,
    initialShowAddSheet: Boolean = false,
    onConsumeInitialShowAddSheet: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val myUserId = remember { SupabaseSessionManager.getUserId(context) ?: "" }

    val canAddMembers = isAdmin || group.whoCanAddMembers.equals("members", ignoreCase = true)

    var membersList by remember { mutableStateOf<List<SupabaseGroupMember>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    val selectedUserIds = remember { mutableStateListOf<String>() }

    var showAddMemberSheet by remember { mutableStateOf(initialShowAddSheet) }
    var activeProfileUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialShowAddSheet) {
        if (initialShowAddSheet) {
            showAddMemberSheet = true
            onConsumeInitialShowAddSheet()
        }
    }

    fun refreshMembers() {
        scope.launch {
            isLoading = true
            loadError = null
            val res = GroupChatRepository.getGroupMembersResult(context, group.id)
            isLoading = false
            res.onSuccess { list ->
                membersList = list
            }.onFailure { err ->
                loadError = err.message
            }
        }
    }

    LaunchedEffect(group.id) {
        refreshMembers()
    }

    LaunchedEffect(group.id) {
        GroupChatRepository.liveGroupUpdates.collect { updatedId ->
            if (updatedId == group.id) {
                refreshMembers()
            }
        }
    }

    BackHandler { onBack() }

    val joinedMembers = remember(membersList) {
        membersList.filter { it.status.equals("joined", ignoreCase = true) }
    }
    val adminMembers = remember(membersList) {
        membersList.filter { it.role.equals("admin", ignoreCase = true) && it.status.equals("joined", ignoreCase = true) }
    }
    val regularMembers = remember(membersList) {
        membersList.filter { !it.role.equals("admin", ignoreCase = true) && it.status.equals("joined", ignoreCase = true) }
    }
    val pendingRequestsAndInvites = remember(membersList) {
        membersList.filter {
            it.status.equals("requested", ignoreCase = true) || it.status.equals("invited", ignoreCase = true)
        }
    }

    val allCount = joinedMembers.size
    val adminCount = adminMembers.size
    val regularCount = regularMembers.size
    val requestCount = pendingRequestsAndInvites.size

    val filteredMembers = remember(membersList, selectedFilter, searchQuery) {
        val baseList = when (selectedFilter) {
            "Admins" -> adminMembers
            "Members" -> regularMembers
            "Requests" -> pendingRequestsAndInvites
            else -> joinedMembers
        }
        baseList.filter { member ->
            val displayName = member.profile_name?.takeIf { it.isNotBlank() }
                ?: member.username?.takeIf { it.isNotBlank() }
                ?: ""
            val uname = member.username.orEmpty()
            searchQuery.isBlank() ||
                displayName.contains(searchQuery, ignoreCase = true) ||
                uname.contains(searchQuery, ignoreCase = true) ||
                member.user_id.contains(searchQuery, ignoreCase = true)
        }
    }

    val isVisibilityRestrictedToAdmins = !isAdmin &&
        (group.memberListVisibility.equals("admins", ignoreCase = true) || (!isLoading && membersList.isEmpty()))

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

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Manage Members",
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "${group.memberCount} joined",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )
                }

                if (canAddMembers) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7))
                            .border(1.dp, Color(0xFFFDE68A), CircleShape)
                            .clickable { showAddMemberSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        GroupPersonAddIcon(size = 20.dp, color = Color(0xFFB45309))
                    }
                } else {
                    Spacer(modifier = Modifier.size(42.dp))
                }
            }
        },
        bottomBar = {
            if (isAdmin && selectedUserIds.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 10.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${selectedUserIds.size} selected",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        var errorText: String? = null
                                        for (uid in selectedUserIds.toList()) {
                                            val res = GroupChatRepository.removeGroupMember(context, group.id, uid)
                                            if (res.isFailure) {
                                                errorText = res.exceptionOrNull()?.message
                                            }
                                        }
                                        selectedUserIds.clear()
                                        refreshMembers()
                                        Toast.makeText(
                                            context,
                                            errorText ?: "Selected members removed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2)),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                GroupTrashIcon(size = 16.dp, color = Color(0xFFEF4444))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Remove", color = Color(0xFFEF4444), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        var errorText: String? = null
                                        for (uid in selectedUserIds.toList()) {
                                            val res = GroupChatRepository.setMemberRole(context, group.id, uid, "admin")
                                            if (res.isFailure) {
                                                errorText = res.exceptionOrNull()?.message
                                            }
                                        }
                                        selectedUserIds.clear()
                                        refreshMembers()
                                        Toast.makeText(
                                            context,
                                            errorText ?: "Promoted to admin",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF3C7)),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                GroupCrownIcon(size = 16.dp, color = Color(0xFFB45309))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Make admin", color = Color(0xFFB45309), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val filters = buildList {
                add("All" to "All ($allCount)")
                add("Admins" to "Admins ($adminCount)")
                add("Members" to "Members ($regularCount)")
                if (isAdmin) {
                    add("Requests" to "Requests ($requestCount)")
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { (key, label) ->
                    val isSelected = selectedFilter == key
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .clickable { selectedFilter = key },
                        shape = RoundedCornerShape(50.dp),
                        color = if (isSelected) Color(0xFFFFC529) else Color(0xFFF1F5F9),
                        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else Color(0xFF475569)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GroupSearchIcon(size = 18.dp, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text("Search members...", color = Color(0xFF94A3B8), fontSize = 13.5.sp)
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 13.5.sp),
                            cursorBrush = SolidColor(Color.Black),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFFC529), modifier = Modifier.size(32.dp))
                }
            } else if (isVisibilityRestrictedToAdmins) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GroupLockIcon(size = 38.dp, color = Color(0xFFD97706))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Member list is visible to admins only",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "The admins of this group have restricted member list visibility.",
                            fontSize = 12.5.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (filteredMembers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GroupCuteEmptyChatIllustration(modifier = Modifier.size(90.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (selectedFilter == "Requests") "No pending requests or invites" else "No members found",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredMembers, key = { "${it.user_id}_${it.status}" }) { member ->
                        val isSelf = member.user_id == myUserId
                        val isMemberAdmin = member.role.equals("admin", ignoreCase = true)
                        val statusLower = member.status.lowercase()
                        val isRequested = statusLower == "requested"
                        val isInvited = statusLower == "invited"
                        val isChecked = selectedUserIds.contains(member.user_id)

                        val baseName = member.profile_name?.takeIf { it.isNotBlank() }
                            ?: member.username?.takeIf { it.isNotBlank() }
                            ?: "Member (${member.user_id.take(6)})"
                        val displayName = if (isSelf) "$baseName (You)" else baseName
                        val avatarUrl = member.avatar_url
                        val isOnline = member.presence_status?.equals("online", ignoreCase = true) == true
                        val subtitleText = when {
                            isRequested -> "Requested to join · Needs approval"
                            isInvited -> "Invited · Waiting to accept"
                            !member.username.isNullOrBlank() -> "@${member.username}"
                            isOnline -> "Online"
                            else -> "Member"
                        }

                        var showMenu by remember { mutableStateOf(false) }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (selectedUserIds.isNotEmpty()) {
                                        if (isChecked) selectedUserIds.remove(member.user_id)
                                        else selectedUserIds.add(member.user_id)
                                    } else {
                                        activeProfileUserId = member.user_id
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isChecked) Color(0xFFFEF9C3) else Color.White,
                            border = BorderStroke(1.dp, if (isChecked) Color(0xFFFDE047) else Color(0xFFF1F5F9))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(modifier = Modifier.size(46.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFEF3C7)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!avatarUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = avatarUrl,
                                                    contentDescription = displayName,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape)
                                                )
                                            } else {
                                                Text(
                                                    text = baseName.take(1).uppercase(),
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFFB45309)
                                                )
                                            }
                                        }

                                        val memberCountry = member.country_code?.takeIf { it.isNotBlank() }
                                            ?: member.country?.takeIf { it.isNotBlank() }
                                            ?: com.example.realtime.VoiceRoomRealtimeManager.getUserSnippet(member.user_id)?.countryCode
                                            ?: "in"
                                        CircleFlag(
                                            countryCode = memberCountry,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .align(Alignment.BottomEnd)
                                                .clip(CircleShape)
                                                .border(1.5.dp, Color.White, CircleShape)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = displayName,
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isOnline) Color(0xFF22C55E) else Color(0xFF94A3B8))
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = subtitleText,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isRequested) Color(0xFFD97706) else Color(0xFF94A3B8),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (isAdmin && isRequested) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    scope.launch {
                                                        val res = GroupChatRepository.approveJoinRequest(
                                                            context,
                                                            group.id,
                                                            member.user_id
                                                        )
                                                        res.onSuccess {
                                                            Toast.makeText(context, "Join request approved", Toast.LENGTH_SHORT).show()
                                                            refreshMembers()
                                                        }.onFailure { err ->
                                                            Toast.makeText(context, err.message ?: "Failed to approve", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                },
                                            color = Color(0xFFFFC529),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = "Approve",
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                        }

                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    scope.launch {
                                                        val res = GroupChatRepository.rejectJoinRequest(
                                                            context,
                                                            group.id,
                                                            member.user_id
                                                        )
                                                        res.onSuccess {
                                                            Toast.makeText(context, "Join request rejected", Toast.LENGTH_SHORT).show()
                                                            refreshMembers()
                                                        }.onFailure { err ->
                                                            Toast.makeText(context, err.message ?: "Failed to reject", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                },
                                            color = Color(0xFFFEE2E2),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = "Reject",
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFEF4444)
                                            )
                                        }
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                                            Spacer(modifier = Modifier.width(6.dp))
                                        } else if (isInvited) {
                                            Surface(
                                                shape = RoundedCornerShape(50.dp),
                                                color = Color(0xFFF1F5F9)
                                            ) {
                                                Text(
                                                    text = "Invited",
                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF64748B)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }

                                        Box {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .clickable { showMenu = true },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                GroupThreeDotsIcon(size = 18.dp, color = Color(0xFF64748B))
                                            }

                                            DropdownMenu(
                                                expanded = showMenu,
                                                onDismissRequest = { showMenu = false },
                                                modifier = Modifier
                                                    .width(220.dp)
                                                    .clip(RoundedCornerShape(18.dp))
                                                    .background(Color.White)
                                            ) {
                                                if (isAdmin && !isSelf && statusLower == "joined") {
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                if (isMemberAdmin) "Dismiss as admin" else "Make admin",
                                                                fontSize = 13.5.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = Color(0xFF0F172A)
                                                            )
                                                        },
                                                        leadingIcon = { GroupCrownIcon(size = 18.dp, color = Color(0xFFD97706)) },
                                                        onClick = {
                                                            showMenu = false
                                                            val targetRole = if (isMemberAdmin) "member" else "admin"
                                                            scope.launch {
                                                                val res = GroupChatRepository.setMemberRole(
                                                                    context,
                                                                    group.id,
                                                                    member.user_id,
                                                                    targetRole
                                                                )
                                                                res.onSuccess {
                                                                    Toast.makeText(
                                                                        context,
                                                                        if (targetRole == "admin") "Promoted to admin" else "Admin role removed",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                    refreshMembers()
                                                                }.onFailure { err ->
                                                                    Toast.makeText(context, err.message ?: "Failed to update role", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                        }
                                                    )
                                                }

                                                if (isAdmin && !isSelf) {
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                if (isInvited) "Cancel invite" else "Remove from group",
                                                                fontSize = 13.5.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = Color(0xFFEF4444)
                                                            )
                                                        },
                                                        leadingIcon = { GroupLeaveDoorIcon(size = 18.dp, color = Color(0xFFEF4444)) },
                                                        onClick = {
                                                            showMenu = false
                                                            scope.launch {
                                                                val res = GroupChatRepository.removeGroupMember(
                                                                    context,
                                                                    group.id,
                                                                    member.user_id
                                                                )
                                                                res.onSuccess {
                                                                    Toast.makeText(
                                                                        context,
                                                                        if (isInvited) "Invite cancelled" else "Removed from group",
                                                                        Toast.LENGTH_SHORT
                                                                    ).show()
                                                                    refreshMembers()
                                                                }.onFailure { err ->
                                                                    Toast.makeText(context, err.message ?: "Failed to remove member", Toast.LENGTH_SHORT).show()
                                                                }
                                                            }
                                                        }
                                                    )
                                                }

                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            "Member details",
                                                            fontSize = 13.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFF0F172A)
                                                        )
                                                    },
                                                    leadingIcon = { GroupEyeIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                                                    onClick = {
                                                        showMenu = false
                                                        activeProfileUserId = member.user_id
                                                    }
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
        }
    }

    if (activeProfileUserId != null) {
        val targetMember = membersList.find { it.user_id == activeProfileUserId }
        val pName = targetMember?.profile_name?.ifBlank { null } ?: targetMember?.username?.ifBlank { null } ?: "Profile"
        val pAvatar = targetMember?.avatar_url ?: ""
        com.example.ui.screens.UserProfileFeedScreen(
            userId = activeProfileUserId!!,
            profileName = pName,
            avatarUrl = pAvatar,
            onBack = { activeProfileUserId = null }
        )
    }

    if (showAddMemberSheet) {
        AddGroupMemberBottomSheet(
            groupId = group.id,
            existingMemberIds = membersList.map { it.user_id }.toSet(),
            onDismiss = { showAddMemberSheet = false },
            onMemberAdded = {
                refreshMembers()
            }
        )
    }
}

private data class AddMemberCandidate(
    val userId: String,
    val displayName: String,
    val username: String? = null,
    val avatarUrl: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroupMemberBottomSheet(
    groupId: String,
    existingMemberIds: Set<String>,
    onDismiss: () -> Unit,
    onMemberAdded: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val myUserId = remember { SupabaseSessionManager.getUserId(context).orEmpty() }

    var searchQuery by remember { mutableStateOf("") }
    var candidates by remember { mutableStateOf<List<AddMemberCandidate>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var addingUserId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                SupabaseSessionManager.ensureActiveSession(context)
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context).orEmpty()
                val loadedMap = linkedMapOf<String, AddMemberCandidate>()

                if (token.isNotBlank() && myUserId.isNotBlank()) {
                    val ids = mutableSetOf<String>()

                    // 1. Direct chat contacts
                    EncryptedChatRepository.conversations.value.forEach { conv ->
                        if (conv.otherUserId.isNotBlank() && conv.otherUserId != myUserId) {
                            ids.add(conv.otherUserId)
                            loadedMap[conv.otherUserId] = AddMemberCandidate(
                                userId = conv.otherUserId,
                                displayName = conv.otherUserName?.takeIf { it.isNotBlank() } ?: "Learner",
                                avatarUrl = conv.otherUserAvatar?.takeIf { it.isNotBlank() }
                            )
                        }
                    }

                    // 2. Following & Followers
                    try {
                        val followingResp = SupabaseClient.api.getFollowers(
                            apiKey = BuildConfig.SUPABASE_ANON_KEY,
                            auth = token,
                            followerIdEq = "eq.$myUserId"
                        )
                        if (followingResp.isSuccessful) {
                            followingResp.body().orEmpty().forEach { f ->
                                if (f.following_id.isNotBlank() && f.following_id != myUserId) {
                                    ids.add(f.following_id)
                                }
                            }
                        }
                    } catch (_: Exception) {}

                    try {
                        val followerResp = SupabaseClient.api.getFollowers(
                            apiKey = BuildConfig.SUPABASE_ANON_KEY,
                            auth = token,
                            followingIdEq = "eq.$myUserId"
                        )
                        if (followerResp.isSuccessful) {
                            followerResp.body().orEmpty().forEach { f ->
                                if (f.follower_id.isNotBlank() && f.follower_id != myUserId) {
                                    ids.add(f.follower_id)
                                }
                            }
                        }
                    } catch (_: Exception) {}

                    // 3. Enrich from discoverable_profiles
                    try {
                        val discResp = SupabaseClient.api.getDiscoverableProfiles(
                            apiKey = BuildConfig.SUPABASE_ANON_KEY,
                            auth = token,
                            options = emptyMap()
                        )
                        if (discResp.isSuccessful) {
                            discResp.body().orEmpty().forEach { p ->
                                val uid = p.auth_user_id
                                if (uid.isNotBlank() && uid != myUserId) {
                                    val name = p.profile_name?.takeIf { it.isNotBlank() }
                                        ?: p.username?.takeIf { it.isNotBlank() }
                                        ?: loadedMap[uid]?.displayName
                                        ?: "Learner"
                                    val avatar = p.avatar_url?.takeIf { it.isNotBlank() }
                                        ?: loadedMap[uid]?.avatarUrl
                                    if (ids.contains(uid) || !loadedMap.containsKey(uid)) {
                                        loadedMap[uid] = AddMemberCandidate(
                                            userId = uid,
                                            displayName = name,
                                            username = p.username,
                                            avatarUrl = avatar
                                        )
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {}

                    // 4. Enrich from profiles table
                    try {
                        val allProfilesRes = SupabaseClient.api.getAllProfiles(
                            apiKey = BuildConfig.SUPABASE_ANON_KEY,
                            auth = token
                        )
                        if (allProfilesRes.isSuccessful) {
                            allProfilesRes.body().orEmpty().forEach { p ->
                                val uid = p.auth_user_id ?: p.id?.toString().orEmpty()
                                if (uid.isNotBlank() && uid != myUserId && (ids.contains(uid) || !loadedMap.containsKey(uid))) {
                                    val existing = loadedMap[uid]
                                    val name = p.profile_name?.takeIf { it.isNotBlank() }
                                        ?: p.username?.takeIf { it.isNotBlank() }
                                        ?: existing?.displayName
                                        ?: "Learner"
                                    val avatar = p.avatar_url?.takeIf { it.isNotBlank() }
                                        ?: existing?.avatarUrl
                                    loadedMap[uid] = AddMemberCandidate(
                                        userId = uid,
                                        displayName = name,
                                        username = p.username ?: existing?.username,
                                        avatarUrl = avatar
                                    )
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }
                candidates = loadedMap.values.toList()
            } catch (_: Exception) {}
        }
        isLoading = false
    }

    val filteredCandidates = remember(candidates, searchQuery, existingMemberIds) {
        candidates.filter { p ->
            val uid = p.userId
            val name = p.displayName
            val uname = p.username.orEmpty()
            val matches = searchQuery.isBlank() ||
                name.contains(searchQuery, ignoreCase = true) ||
                uname.contains(searchQuery, ignoreCase = true)
            uid.isNotBlank() && matches
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
                .fillMaxHeight(0.8f)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Add Members",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Invite your connections or community members to this group.",
                fontSize = 12.5.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name or @username...") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFFC529))
                }
            } else if (filteredCandidates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No matching users found", color = Color(0xFF64748B), fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCandidates, key = { it.userId }) { profile ->
                        val uid = profile.userId
                        val name = profile.displayName.ifBlank { "User" }
                        val alreadyInGroup = existingMemberIds.contains(uid)
                        val isAddingThis = addingUserId == uid

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFEF3C7)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!profile.avatarUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = profile.avatarUrl,
                                                contentDescription = name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                            )
                                        } else {
                                            Text(
                                                text = name.take(1).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFB45309)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (!profile.username.isNullOrBlank()) {
                                            Text(
                                                text = "@${profile.username}",
                                                fontSize = 12.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }

                                if (alreadyInGroup) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFE2E8F0)
                                    ) {
                                        Text(
                                            text = "Added",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                addingUserId = uid
                                                val res = GroupChatRepository.addGroupMember(context, groupId, uid)
                                                addingUserId = null
                                                res.onSuccess {
                                                    Toast.makeText(context, "Invitation sent to $name", Toast.LENGTH_SHORT).show()
                                                    onMemberAdded()
                                                }.onFailure { err ->
                                                    Toast.makeText(context, err.message ?: "Failed to add member", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        enabled = addingUserId == null,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFFC529),
                                            contentColor = Color.Black
                                        ),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isAddingThis) "..." else "Invite",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold
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
}
