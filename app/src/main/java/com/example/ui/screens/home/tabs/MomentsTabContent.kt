package com.example.ui.screens.home.tabs

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.SupabaseClient
import com.example.SupabaseMomentLike
import com.example.SupabaseMomentComment
import com.example.SupabaseSessionManager
import com.example.CloudinaryUploadResponse
import com.example.ui.components.*
import com.example.ui.screens.home.models.MomentPost
import com.example.ui.screens.post.DetailedPost
import com.example.ui.screens.post.PostComment
import com.example.ui.screens.post.PostDetailScreen
import com.example.ui.screens.post.CreatePostScreen
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun MomentsTabContent(
    momentsPosts: List<MomentPost>,
    onToggleLike: (MomentPost) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val selfUserId = remember(context) { SupabaseSessionManager.getUserId(context) ?: "" }

    var selectedPostForDetail by remember { mutableStateOf<DetailedPost?>(null) }
    var selectedTabIndex by remember { mutableStateOf(0) }

    // Live Supabase moments list
    val liveMoments = remember { mutableStateListOf<MomentPost>() }
    var isLoadingMoments by remember { mutableStateOf(true) }

    // Create moment dialog state
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPostCaption by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    fun fetchMoments() {
        coroutineScope.launch {
            try {
                isLoadingMoments = true
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    ?: SupabaseSessionManager.getBearerToken(context)
                    ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"

                val resp = withContext(Dispatchers.IO) {
                    SupabaseClient.api.getAllMoments(
                        apiKey = BuildConfig.SUPABASE_ANON_KEY,
                        auth = token
                    )
                }

                if (resp.isSuccessful) {
                    val rawMoments = resp.body().orEmpty()
                    val mappedList = mutableListOf<MomentPost>()

                    for (m in rawMoments) {
                        var authorName = "FunkyTalk User"
                        var authorUsername = "user"
                        var authorAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"

                        if (!m.user_id.isNullOrBlank()) {
                            try {
                                val profResp = withContext(Dispatchers.IO) {
                                    SupabaseClient.api.getDiscoverableProfiles(
                                        apiKey = BuildConfig.SUPABASE_ANON_KEY,
                                        auth = token,
                                        options = mapOf("auth_user_id" to "eq.${m.user_id}")
                                    )
                                }
                                if (profResp.isSuccessful) {
                                    val prof = profResp.body()?.firstOrNull()
                                    if (prof != null) {
                                        authorName = prof.profile_name ?: prof.username ?: authorName
                                        authorUsername = prof.username ?: authorName.lowercase().replace(" ", "")
                                        if (!prof.avatar_url.isNullOrBlank()) {
                                            authorAvatar = prof.avatar_url!!
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                        }

                        var isLikedByMe = false
                        if (selfUserId.isNotBlank()) {
                            try {
                                val likeResp = withContext(Dispatchers.IO) {
                                    SupabaseClient.api.getMomentLikes(
                                        apiKey = BuildConfig.SUPABASE_ANON_KEY,
                                        auth = token,
                                        momentIdEq = "eq.${m.id}",
                                        userIdEq = "eq.$selfUserId"
                                    )
                                }
                                if (likeResp.isSuccessful) {
                                    isLikedByMe = !likeResp.body().isNullOrEmpty()
                                }
                            } catch (_: Exception) {}
                        }

                        val firstImg = m.image_urls?.firstOrNull()
                        mappedList.add(
                            MomentPost(
                                id = m.id,
                                author = authorName,
                                username = authorUsername,
                                avatar = authorAvatar,
                                timeAgo = "Recently",
                                content = m.content.orEmpty(),
                                imageUrl = firstImg,
                                initialLikes = m.likes_count ?: 0,
                                commentsCount = m.comments_count ?: 0,
                                isLiked = isLikedByMe
                            )
                        )
                    }

                    liveMoments.clear()
                    liveMoments.addAll(mappedList)
                }
            } catch (e: Exception) {
                Log.e("MomentsTab", "Error loading moments", e)
            } finally {
                isLoadingMoments = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchMoments()
    }

    val displayList = if (liveMoments.isNotEmpty()) liveMoments else momentsPosts

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFAFAEE))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Header matching design exactly
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Funky",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFC107)
                    )
                    Text(
                        text = "Talk",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .clickable { Toast.makeText(context, "Filter options", Toast.LENGTH_SHORT).show() },
                        contentAlignment = Alignment.Center
                    ) {
                        SoftFilterIcon(size = 18.dp, color = Color(0xFF0F172A))
                    }
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { showCreateDialog = true },
                        color = Color(0xFFFFD43B),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SoftPlusIcon(size = 16.dp, color = Color(0xFF0F172A))
                            Text(
                                text = "Post",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = Color(0xFF0F172A),
                divider = { HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp) }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Trending", fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal, color = if (selectedTabIndex == 0) Color(0xFF0F172A) else Color(0xFF64748B)) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("My Language", fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal, color = if (selectedTabIndex == 1) Color(0xFF0F172A) else Color(0xFF64748B)) }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                if (isLoadingMoments && liveMoments.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFFFD43B))
                        }
                    }
                } else {
                    items(displayList) { post ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp, horizontal = 14.dp)
                                .clickable {
                                    coroutineScope.launch {
                                        val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                                            ?: SupabaseSessionManager.getBearerToken(context)
                                            ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"

                                        val loadedComments = mutableListOf<PostComment>()
                                        try {
                                            val commentsResp = withContext(Dispatchers.IO) {
                                                SupabaseClient.api.getMomentComments(
                                                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                                                    auth = token,
                                                    momentIdEq = "eq.${post.id}"
                                                )
                                            }
                                            if (commentsResp.isSuccessful) {
                                                val rawComments = commentsResp.body().orEmpty()
                                                for (c in rawComments) {
                                                    var cName = "User"
                                                    var cUname = "user"
                                                    var cAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"
                                                    try {
                                                        val pResp = withContext(Dispatchers.IO) {
                                                            SupabaseClient.api.getDiscoverableProfiles(
                                                                apiKey = BuildConfig.SUPABASE_ANON_KEY,
                                                                auth = token,
                                                                options = mapOf("auth_user_id" to "eq.${c.user_id}")
                                                            )
                                                        }
                                                        if (pResp.isSuccessful) {
                                                            val p = pResp.body()?.firstOrNull()
                                                            if (p != null) {
                                                                cName = p.profile_name ?: p.username ?: cName
                                                                cUname = p.username ?: cName.lowercase().replace(" ", "")
                                                                if (!p.avatar_url.isNullOrBlank()) cAvatar = p.avatar_url!!
                                                            }
                                                        }
                                                    } catch (_: Exception) {}

                                                    loadedComments.add(
                                                        PostComment(
                                                            id = c.id ?: UUID.randomUUID().toString(),
                                                            authorName = cName,
                                                            username = cUname,
                                                            avatarUrl = cAvatar,
                                                            timeAgo = "Recently",
                                                            text = c.content,
                                                            likesCount = 0,
                                                            isLiked = false
                                                        )
                                                    )
                                                }
                                            }
                                        } catch (e: Exception) {
                                            Log.e("MomentsTab", "Error loading comments", e)
                                        }

                                        selectedPostForDetail = DetailedPost(
                                            id = post.id,
                                            authorName = post.author,
                                            authorUsername = post.username,
                                            authorAvatarUrl = post.avatar,
                                            isAuthorVerified = true,
                                            location = "Seoul, South Korea",
                                            timeAgo = post.timeAgo,
                                            images = if (post.imageUrl != null) listOf(post.imageUrl) else listOf("https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=1000"),
                                            caption = post.content,
                                            likesCount = post.initialLikes,
                                            isLiked = post.isLiked,
                                            isBookmarked = false,
                                            initialComments = loadedComments
                                        )
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(18.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                // Author Header Row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = post.avatar,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(post.author, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 15.sp)
                                            SoftVerifiedBadge(size = 14.dp)
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text("@" + post.username + " • Seoul, South Korea • " + post.timeAgo, color = Color(0xFF64748B), fontSize = 12.sp)
                                    }
                                    SoftThreeDotsIcon(size = 18.dp, color = Color(0xFF94A3B8))
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(post.content, color = Color(0xFF1E293B), fontSize = 14.sp, lineHeight = 20.sp)

                                if (post.imageUrl != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(220.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFFE2E8F0))
                                    ) {
                                        AsyncImage(
                                            model = post.imageUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Language tags (Clean pill design, zero emojis)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                                            Text("Korean (Native)", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
                                        }
                                    }

                                    Surface(
                                        color = Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF3B82F6)))
                                            Text("English (Intermediate)", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1E293B))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Interaction Action Bar (Like, Comment, Share, Bookmark)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                coroutineScope.launch {
                                                    val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                                                        ?: SupabaseSessionManager.getBearerToken(context)
                                                        ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"

                                                    val targetState = !post.isLiked
                                                    post.isLiked = targetState
                                                    if (targetState) {
                                                        try {
                                                            withContext(Dispatchers.IO) {
                                                                SupabaseClient.api.likeMoment(
                                                                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                                                                    auth = token,
                                                                    like = SupabaseMomentLike(moment_id = post.id, user_id = selfUserId)
                                                                )
                                                            }
                                                        } catch (_: Exception) {}
                                                    } else {
                                                        try {
                                                            withContext(Dispatchers.IO) {
                                                                SupabaseClient.api.unlikeMoment(
                                                                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                                                                    auth = token,
                                                                    momentIdEq = "eq.${post.id}",
                                                                    userIdEq = "eq.$selfUserId"
                                                                )
                                                            }
                                                        } catch (_: Exception) {}
                                                    }
                                                    onToggleLike(post)
                                                }
                                            }
                                            .padding(6.dp)
                                    ) {
                                        SoftHeartIcon(isLiked = post.isLiked, size = 20.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${if (post.isLiked) post.initialLikes + 1 else post.initialLikes}",
                                            color = Color(0xFF0F172A),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(6.dp)
                                    ) {
                                        SoftChatBubbleIcon(size = 20.dp, color = Color(0xFF64748B))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${post.commentsCount}", color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }

                                    SoftShareIcon(size = 20.dp, color = Color(0xFF64748B))
                                    SoftBookmarkIcon(size = 20.dp, isBookmarked = false, color = Color(0xFF64748B))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Create Post Screen
        if (showCreateDialog) {
            CreatePostScreen(
                onDismiss = { showCreateDialog = false },
                onPostCreated = {
                    showCreateDialog = false
                    fetchMoments()
                }
            )
        }

        // Fullscreen Post Detail overlay
        selectedPostForDetail?.let { detailedPost ->
            PostDetailScreen(
                post = detailedPost,
                currentLoggedInAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=300",
                onBack = { selectedPostForDetail = null },
                onLikePostToggle = { isLiked ->
                    val found = liveMoments.find { it.id == detailedPost.id }
                    if (found != null && found.isLiked != isLiked) {
                        found.isLiked = isLiked
                        onToggleLike(found)
                    }
                }
            )
        }
    }
}
