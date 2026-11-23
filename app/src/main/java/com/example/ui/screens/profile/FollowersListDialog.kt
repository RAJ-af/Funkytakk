package com.example.ui.screens.profile

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.DiscoverableProfile
import com.example.SupabaseClient
import com.example.data.AppCacheStore
import com.example.ui.components.AppNetworkManager
import com.example.ui.components.FollowersSkeleton
import com.example.ui.components.FriendlyErrorRetryCard
import com.example.ui.components.SoftCloseIcon
import com.example.ui.theme.LocalFunkyColors
import com.example.util.ErrorMapper
import com.example.util.StartupTimingTracker
import com.example.util.withLoadingTimeout
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
    val funkyColors = LocalFunkyColors.current
    val isOnline by AppNetworkManager.isOnline.collectAsState()
    val cacheKey = remember(userId, showFollowers) {
        "followers_list_${userId}_${if (showFollowers) "followers" else "following"}"
    }
    val initialCached = remember(cacheKey) {
        AppCacheStore.getSync<List<DiscoverableProfile>>(
            context = context,
            key = cacheKey,
            type = AppCacheStore.discoverableProfileListType()
        )?.data
    }

    var isLoading by remember(cacheKey) { mutableStateOf(initialCached == null) }
    var errorMessage by remember(cacheKey) { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }
    val userList = remember(cacheKey) {
        mutableStateListOf<DiscoverableProfile>().apply {
            if (!initialCached.isNullOrEmpty()) addAll(initialCached)
        }
    }
    val blockedUserIds by com.example.repository.BlockRepository.blockedUserIds.collectAsState()
    val visibleUsers = remember(userList.toList(), blockedUserIds) {
        userList.filterNot { blockedUserIds.contains(it.auth_user_id) }
    }

    LaunchedEffect(userId, showFollowers, retryTrigger) {
        if (userList.isEmpty()) isLoading = true
        errorMessage = null

        try {
            val loaded = withLoadingTimeout {
                withContext(Dispatchers.IO) {
                    val relationRes = StartupTimingTracker.measure("followers_relations") {
                        ErrorMapper.executeWithSingle401Retry(context) { auth ->
                            if (showFollowers) {
                                SupabaseClient.api.getFollowers(
                                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                                    auth = auth,
                                    followingIdEq = "eq.$userId"
                                )
                            } else {
                                SupabaseClient.api.getFollowers(
                                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                                    auth = auth,
                                    followerIdEq = "eq.$userId"
                                )
                            }
                        }
                    }

                    val relationResp = relationRes.getOrElse { throw it }
                    val relations = relationResp.body().orEmpty()
                    val targetIds = relations.map {
                        if (showFollowers) it.follower_id else it.following_id
                    }.toSet()

                    if (targetIds.isEmpty()) {
                        emptyList()
                    } else {
                        val profilesRes = StartupTimingTracker.measure("followers_profiles") {
                            ErrorMapper.executeWithSingle401Retry(context) { auth ->
                                SupabaseClient.api.getDiscoverableProfiles(
                                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                                    auth = auth,
                                    options = emptyMap()
                                )
                            }
                        }
                        val profilesResp = profilesRes.getOrElse { throw it }
                        val allProfiles = profilesResp.body().orEmpty()
                        allProfiles.filter { targetIds.contains(it.auth_user_id) }
                    }
                }
            }

            userList.clear()
            userList.addAll(loaded)
            AppCacheStore.put(
                context = context,
                key = cacheKey,
                value = loaded,
                type = AppCacheStore.discoverableProfileListType()
            )
        } catch (ce: kotlinx.coroutines.CancellationException) {
            throw ce
        } catch (t: Throwable) {
            if (userList.isEmpty()) {
                errorMessage = ErrorMapper.map(t)
            }
        } finally {
            isLoading = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = funkyColors.surface,
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
                    color = funkyColors.ink
                )
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(funkyColors.soft)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    SoftCloseIcon(size = 14.dp, color = funkyColors.mute)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading && userList.isEmpty()) {
                FollowersSkeleton(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .testTag("followers_skeleton")
                )
            } else if (errorMessage != null && userList.isEmpty()) {
                FriendlyErrorRetryCard(
                    message = errorMessage ?: ErrorMapper.MSG_GENERIC,
                    onRetry = { retryTrigger++ },
                    isOnline = isOnline,
                    testTagPrefix = "followers",
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else if (visibleUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (showFollowers) "No followers yet." else "Not following anyone yet.",
                        color = funkyColors.mute,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                val context = androidx.compose.ui.platform.LocalContext.current
                LaunchedEffect(visibleUsers) {
                    val uids = visibleUsers.map { it.auth_user_id }.filter { it.isNotBlank() }
                    if (uids.isNotEmpty()) {
                        com.example.store.CosmeticsManager.batchFetchUserCosmetics(context, uids)
                    }
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(visibleUsers, key = { it.auth_user_id }) { user ->
                        val targetUid = user.auth_user_id
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
                                countryCode = user.country_code,
                                userId = targetUid,
                                showFlagBadge = true,
                                enableFrameAnimation = false
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Names & Username
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                com.example.store.DecoratedUserName(
                                    userId = targetUid,
                                    name = user.profile_name ?: user.username ?: "Anonymous",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    defaultColor = funkyColors.ink
                                )
                                Text(
                                    text = "@${user.username ?: "anonymous"}",
                                    fontSize = 12.sp,
                                    color = funkyColors.mute,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
