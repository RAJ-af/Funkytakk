package com.example.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.lazy.LazyRow
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
import kotlinx.coroutines.launch

val SAMPLE_SHARED_MEDIA = listOf(
    "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=200",
    "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=200",
    "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=200",
    "https://images.unsplash.com/photo-1569718212165-3a8278d5f624?w=200",
    "https://images.unsplash.com/photo-1542051841857-5f90071e7989?w=200"
)

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

    var membersList by remember { mutableStateOf<List<SupabaseGroupMember>>(emptyList()) }
    var memberProfiles by remember { mutableStateOf<Map<String, Pair<String, String?>>>(emptyMap()) }
    var memberLanguages by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoadingMembers by remember { mutableStateOf(true) }

    var showTopMenu by remember { mutableStateOf(false) }
    var memberSearchFilter by remember { mutableStateOf("") }
    var isSearchingMembers by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            Toast.makeText(context, "Group icon updated", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler { onBack() }

    LaunchedEffect(group.id) {
        val list = GroupChatRepository.getGroupMembers(context, group.id)
        membersList = list
        isLoadingMembers = false

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

                Box {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .clickable { showTopMenu = true },
                        contentAlignment = Alignment.Center
                    ) {
                        GroupThreeDotsIcon(size = 20.dp, color = Color(0xFF0F172A))
                    }

                    DropdownMenu(
                        expanded = showTopMenu,
                        onDismissRequest = { showTopMenu = false },
                        modifier = Modifier
                            .width(200.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Invite link", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A)) },
                            leadingIcon = { GroupLinkIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                            onClick = {
                                showTopMenu = false
                                copyInviteLink(context, group.id)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Group settings", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A)) },
                            leadingIcon = { GroupGearIcon(size = 18.dp, color = Color(0xFF0F172A)) },
                            onClick = {
                                showTopMenu = false
                                onNavigateToGroupSettings()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Leave group", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFEF4444)) },
                            leadingIcon = { GroupLeaveDoorIcon(size = 18.dp, color = Color(0xFFEF4444)) },
                            onClick = {
                                showTopMenu = false
                                onLeaveGroupClick()
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero section: Large avatar with camera badge
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(96.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .border(3.dp, Color(0xFFFFC529), CircleShape)
                        .background(Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    if (group.avatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = group.avatarUrl,
                            contentDescription = "Group Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        GroupUsersIcon(size = 44.dp, color = Color(0xFFB45309))
                    }
                }

                if (isAdmin) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color(0xFFFFC529))
                            .border(2.dp, Color.White, CircleShape)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        GroupCameraIcon(size = 14.dp, color = Color.Black)
                    }
                }
            }

            // Group Name & Admin Pill
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = group.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
                if (isAdmin) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFEF3C7)
                    ) {
                        Text(
                            text = "ADMIN",
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFB45309)
                        )
                    }
                }
            }

            // Subtitle: @name · X members
            val totalMemberCount = membersList.size.coerceAtLeast(group.memberCount).coerceAtLeast(1)
            Text(
                text = "@${group.name.lowercase().replace(" ", "_")} · $totalMemberCount members",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )

            // E2EE tag
            Row(verticalAlignment = Alignment.CenterVertically) {
                GroupTinyLockIcon(size = 11.dp, color = Color(0xFFD97706))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "End-to-end encrypted",
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }

            // Action Buttons: Add members & Invite link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onAddMemberClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC529)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    GroupPersonAddIcon(size = 18.dp, color = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add members",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }

                OutlinedButton(
                    onClick = { copyInviteLink(context, group.id) },
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(50.dp),
                    border = BorderStroke(1.2.dp, Color(0xFFCBD5E1)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                ) {
                    GroupLinkIcon(size = 18.dp, color = Color(0xFF0F172A))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Invite link",
                        color = Color(0xFF0F172A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
            }

            // Card 1: About
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToGroupSettings() }
                        .padding(16.dp)
                ) {
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
                        GroupChevronRightIcon(size = 14.dp, color = Color(0xFF94A3B8))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Language practice & casual conversations.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 18.sp
                    )
                }
            }

            // Card 2: Settings List Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    // Notifications
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Toast.makeText(context, "Notification settings", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GroupBellIcon(size = 18.dp, color = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Notifications", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("All messages", fontSize = 12.5.sp, color = Color(0xFF94A3B8))
                            Spacer(modifier = Modifier.width(6.dp))
                            GroupChevronRightIcon(size = 14.dp, color = Color(0xFF94A3B8))
                        }
                    }

                    HorizontalDivider(color = Color(0xFFF8FAFC))

                    // Manage members
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToManageMembers() }
                            .padding(vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GroupUsersIcon(size = 18.dp, color = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Manage members", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                        }
                        GroupChevronRightIcon(size = 14.dp, color = Color(0xFF94A3B8))
                    }

                    HorizontalDivider(color = Color(0xFFF8FAFC))

                    // Group settings
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToGroupSettings() }
                            .padding(vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GroupGearIcon(size = 18.dp, color = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Group settings", fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                        }
                        GroupChevronRightIcon(size = 14.dp, color = Color(0xFF94A3B8))
                    }
                }
            }

            // Card 3: Members Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
                                text = "Members ($totalMemberCount)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        // Search pill button
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.clickable { onNavigateToManageMembers() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GroupSearchIcon(size = 14.dp, color = Color(0xFF64748B))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Search members", fontSize = 11.5.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isLoadingMembers) {
                        Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFFFFC529), modifier = Modifier.size(24.dp))
                        }
                    } else {
                        val displayList = membersList.take(6)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (member in displayList) {
                                val isSelf = member.user_id == myUserId
                                val isMemberAdmin = member.role == "admin"
                                val prof = memberProfiles[member.user_id]
                                val name = if (isSelf) "You" else prof?.first ?: "Member (${member.user_id.take(6)})"
                                val avatarUrl = prof?.second
                                val langCode = memberLanguages[member.user_id] ?: "EN · HI"

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Avatar with green status dot
                                        Box(modifier = Modifier.size(42.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
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
                                                    GroupUsersIcon(size = 20.dp, color = Color(0xFFB45309))
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(11.dp)
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
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = langCode,
                                                fontSize = 11.sp,
                                                color = Color(0xFF94A3B8),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isMemberAdmin) {
                                            Surface(
                                                shape = RoundedCornerShape(50.dp),
                                                color = Color(0xFFFEF3C7)
                                            ) {
                                                Text(
                                                    text = "Admin",
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB45309)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .clickable { onNavigateToManageMembers() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            GroupThreeDotsIcon(size = 16.dp, color = Color(0xFF64748B))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Card 4: Shared media (12) -> View all >
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
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
                                text = "Shared media (12)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                Toast.makeText(context, "Opening all shared media...", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("View all", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.width(4.dp))
                            GroupChevronRightIcon(size = 13.dp, color = Color(0xFF94A3B8))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(SAMPLE_SHARED_MEDIA) { url ->
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF1F5F9))
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Media",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Last "+8" tile
                        item {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFF78716C)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+8", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun copyInviteLink(context: Context, groupId: String) {
    val inviteUrl = "https://funkytalk.app/join/$groupId"
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Group Invite Link", inviteUrl)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Invite link copied to clipboard!", Toast.LENGTH_SHORT).show()
}
