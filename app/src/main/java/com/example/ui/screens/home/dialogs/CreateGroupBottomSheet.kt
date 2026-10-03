package com.example.ui.screens.home.dialogs

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ChatPreferencesManager
import com.example.LocalGroupChat
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.ui.components.*
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.launch

val GROUP_AVATAR_PRESETS = listOf(
    "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=200",
    "https://images.unsplash.com/photo-1529156069898-49953e39b3ac?w=200",
    "https://images.unsplash.com/photo-1517048676732-d65bc937f952?w=200",
    "https://images.unsplash.com/photo-1543269865-cbf427effbad?w=200",
    "https://images.unsplash.com/photo-1521737604893-d14cc237f11d?w=200"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupBottomSheet(
    onDismiss: () -> Unit,
    onGroupCreated: (LocalGroupChat) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf(GROUP_AVATAR_PRESETS[0]) }

    var memberInputText by remember { mutableStateOf("") }
    val selectedMembers = remember { mutableStateListOf<String>() }
    val suggestedFriends = remember { mutableStateListOf<Pair<String, String>>() } // id, name
    val friendNameToIdMap = remember { mutableStateMapOf<String, String>() }
    var isCreating by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Load actual profiles to suggest as members
    LaunchedEffect(Unit) {
        try {
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
            if (!token.isNullOrBlank()) {
                val profilesRes = SupabaseClient.api.getAllProfiles(
                    apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                    auth = token
                )
                if (profilesRes.isSuccessful && profilesRes.body() != null) {
                    val myId = SupabaseSessionManager.getUserId(context)
                    val otherProfiles = profilesRes.body()!!
                        .filterNot { it.auth_user_id == myId }
                        .take(15)
                        .map { (it.auth_user_id ?: it.id.toString()) to (it.profile_name ?: it.username ?: "Partner") }
                    suggestedFriends.clear()
                    suggestedFriends.addAll(otherProfiles)
                    friendNameToIdMap.clear()
                    otherProfiles.forEach { (id, name) -> friendNameToIdMap[name] = id }
                }
            }
        } catch (_: Exception) {}
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        GroupUsersIcon(size = 20.dp, color = Color(0xFFB45309))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Create Group Chat",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6))
                ) {
                    GroupCloseIcon(size = 14.dp, color = Color(0xFF374151))
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Avatar Selection
            Text(
                text = "Group Icon",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(GROUP_AVATAR_PRESETS) { url ->
                    val isSelected = selectedAvatar == url
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .border(
                                BorderStroke(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) GoldAccent else Color(0xFFE5E7EB)
                                ),
                                CircleShape
                            )
                            .clickable { selectedAvatar = url },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Group Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(GoldAccent)
                                    .border(1.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                GroupCheckIcon(size = 10.dp, color = Color.Black)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Group Name Input
            Text(
                text = "Group Name",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF9FAFB))
                    .border(BorderStroke(1.dp, Color(0xFFE5E7EB)), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (groupName.isEmpty()) {
                    Text(
                        text = "e.g. Anime Club, Japanese Learners...",
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(Color.Black),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Topic / Description
            Text(
                text = "Topic / Description (Optional)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF9FAFB))
                    .border(BorderStroke(1.dp, Color(0xFFE5E7EB)), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (groupDescription.isEmpty()) {
                    Text(
                        text = "What is this group about?",
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = groupDescription,
                    onValueChange = { groupDescription = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(Color.Black),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Add Members
            Text(
                text = "Add Members",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151)
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Chips of added members
            if (selectedMembers.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    items(selectedMembers) { member ->
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = member,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { selectedMembers.remove(member) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    GroupCloseIcon(size = 10.dp, color = Color(0xFF92400E))
                                }
                            }
                        }
                    }
                }
            }

            // Member input field
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF9FAFB))
                        .border(BorderStroke(1.dp, Color(0xFFE5E7EB)), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (memberInputText.isEmpty()) {
                        Text(
                            text = "Type partner name or username...",
                            color = Color(0xFF9CA3AF),
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = memberInputText,
                        onValueChange = { memberInputText = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.Black, fontSize = 13.sp),
                        cursorBrush = SolidColor(Color.Black),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val trimmed = memberInputText.trim()
                        if (trimmed.isNotBlank() && !selectedMembers.contains(trimmed)) {
                            selectedMembers.add(trimmed)
                            memberInputText = ""
                        }
                    },
                    enabled = memberInputText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text("Add", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Suggested Friends row
            if (suggestedFriends.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(suggestedFriends) { (_, name) ->
                        val isAdded = selectedMembers.contains(name)
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = if (isAdded) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isAdded) Color(0xFFFDE68A) else Color(0xFFE2E8F0)),
                            modifier = Modifier.clickable {
                                if (isAdded) {
                                    selectedMembers.remove(name)
                                } else {
                                    selectedMembers.add(name)
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isAdded) {
                                    GroupCheckIcon(size = 11.dp, color = Color(0xFFB45309))
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = name,
                                    fontSize = 12.sp,
                                    color = if (isAdded) Color(0xFF92400E) else Color(0xFF334155),
                                    fontWeight = if (isAdded) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Create Group Button
            Button(
                onClick = {
                    if (groupName.isBlank()) {
                        Toast.makeText(context, "Please enter a group name", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isCreating = true
                    val memberIds = selectedMembers.mapNotNull { friendNameToIdMap[it] }

                    scope.launch {
                        val serverRes = com.example.repository.GroupChatRepository.createGroup(
                            context = context,
                            name = groupName.trim(),
                            memberUserIds = memberIds,
                            avatarUrl = selectedAvatar
                        )
                        isCreating = false

                        val realId = serverRes.getOrNull()
                        val created = ChatPreferencesManager.createGroup(
                            context = context,
                            name = groupName.trim(),
                            description = groupDescription.trim(),
                            memberNames = selectedMembers.toList(),
                            avatarUrl = selectedAvatar,
                            groupId = realId
                        )

                        if (serverRes.isSuccess) {
                            com.example.repository.GroupChatRepository.syncUserGroups(context)
                            Toast.makeText(context, "Group \"${groupName.trim()}\" created & invitations sent!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Group created: ${serverRes.exceptionOrNull()?.message ?: ""}", Toast.LENGTH_SHORT).show()
                        }
                        onGroupCreated(created)
                        onDismiss()
                    }
                },
                enabled = !isCreating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Creating Group...", color = Color.Black, fontWeight = FontWeight.Bold)
                } else {
                    GroupUsersIcon(size = 20.dp, color = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Create Group Chat",
                        color = Color.Black,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
