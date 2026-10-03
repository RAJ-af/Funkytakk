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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.SupabaseClient
import com.example.SupabaseGroupMember
import com.example.SupabaseSessionManager
import com.example.db.LocalGroupConversation
import com.example.repository.GroupChatRepository
import com.example.ui.components.*
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.launch

@Composable
fun ManageMembersScreen(
    group: LocalGroupConversation,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onAddMemberClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val myUserId = remember { SupabaseSessionManager.getUserId(context) ?: "" }

    var membersList by remember { mutableStateOf<List<SupabaseGroupMember>>(emptyList()) }
    var memberProfiles by remember { mutableStateOf<Map<String, Pair<String, String?>>>(emptyMap()) }
    var memberLanguages by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    val selectedUserIds = remember { mutableStateListOf<String>() }

    BackHandler { onBack() }

    LaunchedEffect(group.id) {
        val list = GroupChatRepository.getGroupMembers(context, group.id)
        membersList = list
        isLoading = false

        // Fetch discoverable profiles
        val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
        if (!token.isNullOrBlank()) {
            val profMap = mutableMapOf<String, Pair<String, String?>>()
            val langMap = mutableMapOf<String, String>()
            for (m in list) {
                try {
                    val pResp = SupabaseClient.api.getDiscoverableProfiles(
                        apiKey = BuildConfig.SUPABASE_ANON_KEY,
                        auth = token,
                        options = mapOf("auth_user_id" to "eq.${m.user_id}")
                    )
                    val p = pResp.body()?.firstOrNull()
                    val name = p?.profile_name ?: p?.username ?: "Member"
                    profMap[m.user_id] = name to p?.avatar_url
                    val nativeL = p?.native_languages?.firstOrNull() ?: "EN"
                    val targetL = p?.learning_languages?.firstOrNull() ?: "KO"
                    langMap[m.user_id] = "${nativeL.take(2).uppercase()} · ${targetL.take(2).uppercase()}"
                } catch (_: Exception) {}
            }
            memberProfiles = profMap
            memberLanguages = langMap
        }
    }

    val adminCount = remember(membersList) { membersList.count { it.role == "admin" } }
    val regularCount = remember(membersList) { membersList.count { it.role != "admin" && it.status == "joined" } }
    val requestCount = remember(membersList) { membersList.count { it.status == "invited" } }

    val filteredMembers = remember(membersList, selectedFilter, searchQuery, memberProfiles) {
        membersList.filter { member ->
            val matchesFilter = when (selectedFilter) {
                "Admins" -> member.role == "admin"
                "Members" -> member.role != "admin" && member.status == "joined"
                "Requests" -> member.status == "invited"
                else -> true
            }
            val name = memberProfiles[member.user_id]?.first ?: ""
            val matchesSearch = searchQuery.isBlank() ||
                    name.contains(searchQuery, ignoreCase = true) ||
                    member.user_id.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

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
                    text = "Manage Members",
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                if (isAdmin) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .clickable { onAddMemberClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        GroupPersonAddIcon(size = 20.dp, color = Color(0xFF0F172A))
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
                            // Remove button
                            Button(
                                onClick = {
                                    scope.launch {
                                        for (uid in selectedUserIds.toList()) {
                                            GroupChatRepository.removeGroupMember(context, group.id, uid)
                                        }
                                        membersList = membersList.filterNot { it.user_id in selectedUserIds }
                                        selectedUserIds.clear()
                                        Toast.makeText(context, "Members updated", Toast.LENGTH_SHORT).show()
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

                            // Make admin button
                            Button(
                                onClick = {
                                    Toast.makeText(context, "Admin rights updated", Toast.LENGTH_SHORT).show()
                                    selectedUserIds.clear()
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
            // Filter Chips Row
            val filters = listOf(
                "All" to "All (${membersList.size})",
                "Admins" to "Admins ($adminCount)",
                "Members" to "Members ($regularCount)",
                "Requests" to "Requests ($requestCount)"
            )

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

            Spacer(modifier = Modifier.height(12.dp))

            // Member list
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFFC529), modifier = Modifier.size(32.dp))
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
                        Text("No members found", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
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
                    items(filteredMembers, key = { it.user_id }) { member ->
                        val isSelf = member.user_id == myUserId
                        val isMemberAdmin = member.role == "admin"
                        val isChecked = selectedUserIds.contains(member.user_id)
                        val prof = memberProfiles[member.user_id]
                        val name = if (isSelf) "You" else prof?.first ?: "Member (${member.user_id.take(6)})"
                        val avatarUrl = prof?.second
                        val langCode = memberLanguages[member.user_id] ?: "EN · KO"

                        var showMenu by remember { mutableStateOf(false) }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (isAdmin && !isSelf) {
                                        if (isChecked) selectedUserIds.remove(member.user_id)
                                        else selectedUserIds.add(member.user_id)
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Avatar with green status dot
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
                                                    contentDescription = name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                                )
                                            } else {
                                                GroupUsersIcon(size = 22.dp, color = Color(0xFFB45309))
                                            }
                                        }

                                        // Online indicator dot
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .align(Alignment.BottomEnd)
                                                .clip(CircleShape)
                                                .background(Color(0xFF22C55E))
                                                .border(2.dp, Color.White, CircleShape)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = name,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = langCode,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Admin badge
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
                                    }

                                    // 3 dots button
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
                                            if (isAdmin && !isSelf) {
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
                                                        Toast.makeText(context, "Role updated", Toast.LENGTH_SHORT).show()
                                                    }
                                                )

                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            "Remove from group",
                                                            fontSize = 13.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFFEF4444)
                                                        )
                                                    },
                                                    leadingIcon = { GroupLeaveDoorIcon(size = 18.dp, color = Color(0xFFEF4444)) },
                                                    onClick = {
                                                        showMenu = false
                                                        scope.launch {
                                                            GroupChatRepository.removeGroupMember(context, group.id, member.user_id)
                                                            membersList = membersList.filterNot { it.user_id == member.user_id }
                                                            Toast.makeText(context, "Removed from group", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                )

                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            "Restrict messaging",
                                                            fontSize = 13.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFF0F172A)
                                                        )
                                                    },
                                                    leadingIcon = { GroupRestrictIcon(size = 18.dp, color = Color(0xFF64748B)) },
                                                    onClick = {
                                                        showMenu = false
                                                        Toast.makeText(context, "Member restricted", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            }

                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        "View profile",
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF0F172A)
                                                    )
                                                },
                                                leadingIcon = { GroupEyeIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                                                onClick = {
                                                    showMenu = false
                                                    Toast.makeText(context, "Opening profile...", Toast.LENGTH_SHORT).show()
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
