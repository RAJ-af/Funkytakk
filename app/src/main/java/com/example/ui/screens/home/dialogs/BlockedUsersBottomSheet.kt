package com.example.ui.screens.home.dialogs

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ChatPreferencesManager
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.ui.components.*
import kotlinx.coroutines.launch

data class BlockedUserItem(
    val id: String,
    val name: String,
    val avatarUrl: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersBottomSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val blockedIds by ChatPreferencesManager.blockedUsers.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val blockedUsersList = remember { mutableStateListOf<BlockedUserItem>() }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(blockedIds) {
        isLoading = true
        try {
            val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
            val profileMap = mutableMapOf<String, Pair<String, String>>()
            if (!token.isNullOrBlank()) {
                val res = SupabaseClient.api.getAllProfiles(
                    apiKey = com.example.BuildConfig.SUPABASE_ANON_KEY,
                    auth = token
                )
                if (res.isSuccessful && res.body() != null) {
                    res.body()!!.forEach { p ->
                        val uid = p.auth_user_id ?: p.id.toString()
                        val name = p.profile_name ?: p.username ?: "Learner"
                        val avatar = p.avatar_url ?: ""
                        profileMap[uid] = name to avatar
                    }
                }
            }

            blockedUsersList.clear()
            blockedIds.forEach { id ->
                val cached = profileMap[id]
                blockedUsersList.add(
                    BlockedUserItem(
                        id = id,
                        name = cached?.first ?: "User (${id.take(8)})",
                        avatarUrl = cached?.second?.ifBlank { "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200" }
                            ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200"
                    )
                )
            }
        } catch (_: Exception) {
            blockedUsersList.clear()
            blockedIds.forEach { id ->
                blockedUsersList.add(
                    BlockedUserItem(
                        id = id,
                        name = "User (${id.take(8)})",
                        avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200"
                    )
                )
            }
        } finally {
            isLoading = false
        }
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
                .padding(horizontal = 22.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2)),
                        contentAlignment = Alignment.Center
                    ) {
                        SoftBlockIcon(size = 20.dp, color = Color(0xFFDC2626))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Blocked Users",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                        Text(
                            text = "${blockedIds.size} user(s) blocked",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6))
                ) {
                    SoftCloseIcon(size = 14.dp, color = Color(0xFF374151))
                }
            }

            HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            if (blockedIds.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF0FDF4)),
                        contentAlignment = Alignment.Center
                    ) {
                        SoftShieldIcon(size = 36.dp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No Blocked Users",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "You haven't blocked anyone. Blocked users will appear here and can be unblocked anytime.",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    items(blockedUsersList, key = { it.id }) { user ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFFFAFAFA),
                            border = BorderStroke(1.dp, Color(0xFFF3F4F6)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatarImage(
                                    avatarUrl = user.avatarUrl,
                                    contentDescription = user.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Blocked from chatting & calls",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF9CA3AF)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        ChatPreferencesManager.unblockUser(context, user.id)
                                        Toast.makeText(context, "${user.name} unblocked successfully", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(50.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFFEF2F2),
                                        contentColor = Color(0xFFDC2626)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                    elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
                                ) {
                                    Text(
                                        text = "Unblock",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
