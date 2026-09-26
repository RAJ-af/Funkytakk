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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
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
import com.example.ui.screens.home.models.MomentPost
import com.example.ui.screens.post.DetailedPost
import com.example.ui.screens.post.PostComment
import com.example.ui.screens.post.PostDetailScreen
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

    // Function to load moments from Supabase
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
                        // Fetch author profile
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

                        // Check if liked by self
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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAEE)),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.White,
                    contentColor = Color(0xFF0F172A)
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = { Text("Trending", fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = { Text("My Language", fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            if (isLoadingMoments && liveMoments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
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
                                    Text(post.author, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), fontSize = 15.sp)
                                    Text("@" + post.username + " • " + post.timeAgo, color = Color(0xFF64748B), fontSize = 12.sp)
                                }
                                Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color(0xFF94A3B8))
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text(post.content, color = Color(0xFF1E293B), fontSize = 14.sp, lineHeight = 20.sp)

                            if (post.imageUrl != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                AsyncImage(
                                    model = post.imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Like interaction
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
                                    Icon(
                                        imageVector = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "Likes",
                                        tint = if (post.isLiked) Color(0xFFE11D48) else Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${if (post.isLiked) post.initialLikes + 1 else post.initialLikes}",
                                        color = Color(0xFF0F172A),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }

                                // Comments counter display & opener
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            // Trigger detail view
                                        }
                                        .padding(6.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.Chat,
                                        contentDescription = "Comments",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${post.commentsCount}", color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }

                                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button to create new post
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = Color(0xFFFFD43B),
            contentColor = Color(0xFF0F172A),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Moment")
        }

        // Create Moment Dialog
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { if (!isUploading) showCreateDialog = false },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp),
                title = { Text("Create New Moment", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newPostCaption,
                            onValueChange = { newPostCaption = it },
                            placeholder = { Text("What's on your mind? Share your language journey...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (selectedImageUri == null) "Choose Image from Gallery" else "Image Selected ✅", color = Color(0xFF0F172A))
                        }

                        if (selectedImageUri != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPostCaption.isBlank() && selectedImageUri == null) {
                                Toast.makeText(context, "Please add text or image", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isUploading = true
                            coroutineScope.launch {
                                try {
                                    var imageUrlStr: String? = null
                                    if (selectedImageUri != null) {
                                        val storageRef = FirebaseStorage.getInstance().reference
                                            .child("moments/${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
                                        storageRef.putFile(selectedImageUri!!).await()
                                        imageUrlStr = storageRef.downloadUrl.await().toString()
                                    }

                                    val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                                        ?: SupabaseSessionManager.getBearerToken(context)
                                        ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"

                                    val momentBody = mapOf<String, Any?>(
                                        "user_id" to selfUserId,
                                        "content" to newPostCaption,
                                        "image_urls" to if (imageUrlStr != null) listOf(imageUrlStr) else emptyList()
                                    )

                                    val resp = withContext(Dispatchers.IO) {
                                        SupabaseClient.api.createMoment(
                                            apiKey = BuildConfig.SUPABASE_ANON_KEY,
                                            auth = token,
                                            moment = momentBody
                                        )
                                    }

                                    if (resp.isSuccessful) {
                                        Toast.makeText(context, "Moment posted successfully!", Toast.LENGTH_SHORT).show()
                                        showCreateDialog = false
                                        newPostCaption = ""
                                        selectedImageUri = null
                                        fetchMoments()
                                    } else {
                                        Toast.makeText(context, "Failed to post moment", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Log.e("MomentsTab", "Error creating moment", e)
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isUploading = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD43B)),
                        enabled = !isUploading
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF0F172A))
                        } else {
                            Text("Post Moment", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { if (!isUploading) showCreateDialog = false }) {
                        Text("Cancel", color = Color(0xFF64748B))
                    }
                }
            )
        }

        // Fullscreen Post Detail overlay with swipeable carousel & live comments
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
