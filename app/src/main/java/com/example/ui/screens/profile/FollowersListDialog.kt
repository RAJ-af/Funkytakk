package com.example.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.DiscoverableProfile
import com.example.SupabaseClient
import com.example.SupabaseFollower
import com.example.SupabaseSessionManager
import com.example.ui.components.CircleFlag
import com.example.ui.components.SoftCloseIcon
import com.example.ui.components.SoftGlobeIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowersListBottomSheet(
    userId: String,
    showFollowers: Boolean, // true = Followers, false = Following
    onDismiss: () -> Unit,
    onUserClick: (profile: DiscoverableProfile) -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val userList = remember { mutableStateListOf<DiscoverableProfile>() }

    LaunchedEffect(userId, showFollowers) {
        isLoading = true
        errorMessage = null
        userList.clear()

        try {
            val token = SupabaseSessionManager.getBearerToken(context)
            
            // 1. Fetch Follower/Following relations
            val relationResp = withContext(Dispatchers.IO) {
                if (showFollowers) {
                    SupabaseClient.api.getFollowers(
                        apiKey = BuildConfig.SUPABASE_ANON_KEY,
                        auth = token,
                        followingIdEq = "eq.$userId"
                    )
                } else {
                    SupabaseClient.api.getFollowers(
                        apiKey = BuildConfig.SUPABASE_ANON_KEY,
                        auth = token,
                        followerIdEq = "eq.$userId"
                    )
                }
            }

            if (relationResp.isSuccessful) {
                val relations = relationResp.body().orEmpty()
                val targetIds = relations.map {
                    if (showFollowers) it.follower_id else it.following_id
                }.toSet()

                if (targetIds.isNotEmpty()) {
                    // 2. Fetch all discoverable profiles
                    val profilesResp = withContext(Dispatchers.IO) {
                        SupabaseClient.api.getDiscoverableProfiles(
                            apiKey = BuildConfig.SUPABASE_ANON_KEY,
                            auth = token,
                            options = emptyMap()
                        )
                    }

                    if (profilesResp.isSuccessful) {
                        val allProfiles = profilesResp.body().orEmpty()
                        val filtered = allProfiles.filter { targetIds.contains(it.auth_user_id) }
                        userList.addAll(filtered)
                    } else {
                        errorMessage = "Failed to load profiles: ${profilesResp.message()}"
                    }
                }
            } else {
                errorMessage = "Failed to fetch relations: ${relationResp.message()}"
            }
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "An error occurred"
        } finally {
            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showFollowers) "Followers" else "Following",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    SoftCloseIcon(size = 14.dp, color = Color(0xFF64748B))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFFD43B))
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color.Red,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (userList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (showFollowers) "No followers yet." else "Not following anyone yet.",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(userList) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    onDismiss()
                                    onUserClick(user)
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            com.example.ui.components.PresenceAvatar(
                                avatarUrl = user.avatar_url,
                                size = 46.dp,
                                isSpeaking = false,
                                isOnline = user.is_online ?: false,
                                countryCode = user.country_code
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Names & Username
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = user.profile_name ?: user.username ?: "Anonymous",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "@${user.username ?: "anonymous"}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Country flag
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                            ) {
                                CircleFlag(
                                    countryCode = user.country_code ?: "us",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
