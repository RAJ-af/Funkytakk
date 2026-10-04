package com.example.ui.screens.home.dialogs

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.ChatPreferencesManager
import com.example.LocalGroupChat
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.repository.EncryptedChatRepository
import com.example.repository.GroupChatRepository
import com.example.ui.components.*
import com.example.ui.theme.GoldAccent
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Curated preset high-resolution group avatar photos matching FunkyTalk theme.
 */
val GROUP_AVATAR_PRESETS = listOf(
    "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=400", // Mountain Sunset
    "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=400", // Aesthetic Library Cafe
    "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=400", // Cozy Cat
    "https://images.unsplash.com/photo-1528164344705-475426879c0d?w=400", // Japanese Cherry Blossom Street
    "https://images.unsplash.com/photo-1529156069898-49953e39b3ac?w=400"  // Sunset Friends Silhouette
)

enum class ContactSource {
    FOLLOWING,
    FOLLOWER,
    CHAT
}

data class EligibleContact(
    val userId: String,
    val displayName: String,
    val username: String? = null,
    val avatarUrl: String? = null,
    val languages: String = "EN • GLOBAL",
    val sources: Set<ContactSource> = emptySet()
)

private fun formatMemberLanguages(nativeLangs: List<String>?, learningLangs: List<String>?): String {
    val codes = mutableListOf<String>()
    nativeLangs?.firstOrNull { it.isNotBlank() }?.let { lang ->
        codes.add(languageToCode(lang))
    }
    learningLangs?.firstOrNull { it.isNotBlank() }?.let { lang ->
        val code = languageToCode(lang)
        if (!codes.contains(code)) {
            codes.add(code)
        }
    }
    if (codes.isEmpty()) return "EN • GLOBAL"
    return codes.joinToString(" • ")
}

private fun languageToCode(lang: String): String {
    val l = lang.trim().lowercase()
    return when {
        l.startsWith("en") || l == "english" -> "EN"
        l.startsWith("hi") || l == "hindi" -> "HI"
        l.startsWith("ko") || l == "korean" -> "KO"
        l.startsWith("fr") || l == "french" -> "FR"
        l.startsWith("ja") || l.startsWith("jp") || l == "japanese" -> "JP"
        l.startsWith("es") || l == "spanish" -> "ES"
        l.startsWith("de") || l == "german" -> "DE"
        l.startsWith("zh") || l.startsWith("cn") || l == "chinese" || l == "mandarin" -> "ZH"
        l.startsWith("ar") || l == "arabic" -> "AR"
        l.startsWith("pt") || l == "portuguese" -> "PT"
        l.startsWith("it") || l == "italian" -> "IT"
        l.startsWith("ru") || l == "russian" -> "RU"
        l.length in 2..3 -> l.uppercase()
        else -> l.take(2).uppercase()
    }
}

private fun Modifier.dashedBorder(
    width: Dp,
    color: Color,
    cornerRadius: Dp,
    dashLength: Dp = 6.dp,
    gapLength: Dp = 6.dp
): Modifier = this.drawBehind {
    val stroke = Stroke(
        width = width.toPx(),
        pathEffect = PathEffect.dashPathEffect(
            floatArrayOf(dashLength.toPx(), gapLength.toPx()),
            0f
        )
    )
    val r = cornerRadius.toPx()
    val halfW = width.toPx() / 2
    drawRoundRect(
        color = color,
        topLeft = Offset(halfW, halfW),
        size = Size(size.width - width.toPx(), size.height - width.toPx()),
        cornerRadius = CornerRadius(r, r),
        style = stroke
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupBottomSheet(
    onDismiss: () -> Unit,
    onGroupCreated: (LocalGroupChat) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Step state: 1 = Group Profile & Info, 2 = Invite Eligible Members
    var currentStep by remember { mutableStateOf(1) }

    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf(GROUP_AVATAR_PRESETS[0]) }
    var customAvatarUri by remember { mutableStateOf<Uri?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    val selectedMemberIds = remember { mutableStateListOf<String>() }
    val eligibleContacts = remember { mutableStateListOf<EligibleContact>() }
    var isLoadingContacts by remember { mutableStateOf(true) }
    var isCreating by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Photo picker launcher for custom group avatar
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            customAvatarUri = uri
            selectedAvatar = uri.toString()
        }
    }

    // Strictly load connections: users you follow, followers, and users you've chatted with
    LaunchedEffect(Unit) {
        isLoadingContacts = true
        withContext(Dispatchers.IO) {
            try {
                val supaUid = SupabaseSessionManager.getUserId(context).orEmpty()
                val fbUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                val currentUserId = if (supaUid.isNotBlank()) supaUid else fbUid
                val token = SupabaseSessionManager.getBearerToken(context)

                val eligibleIds = mutableSetOf<String>()
                val contactMap = mutableMapOf<String, EligibleContact>()
                val sourceMap = mutableMapOf<String, MutableSet<ContactSource>>()

                // 1. Chatted users from local Room DB / EncryptedChatRepository
                try {
                    val dbConversations = EncryptedChatRepository.conversations.value
                    dbConversations.forEach { conv ->
                        if (conv.otherUserId.isNotBlank() && conv.otherUserId != currentUserId && conv.otherUserId != supaUid && conv.otherUserId != fbUid) {
                            val uid = conv.otherUserId
                            eligibleIds.add(uid)
                            sourceMap.getOrPut(uid) { mutableSetOf() }.add(ContactSource.CHAT)
                            contactMap[uid] = EligibleContact(
                                userId = uid,
                                displayName = conv.otherUserName?.ifBlank { null } ?: "Learner",
                                avatarUrl = conv.otherUserAvatar?.ifBlank { null },
                                sources = sourceMap[uid] ?: setOf(ContactSource.CHAT)
                            )
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("CreateGroup", "Error reading chat contacts: ${e.message}")
                }

                // 2. Following users (people the user follows)
                val searchUserIds = listOfNotNull(supaUid.takeIf { it.isNotBlank() }, fbUid.takeIf { it.isNotBlank() }).distinct()
                for (uid in searchUserIds) {
                    try {
                        val followingRes = SupabaseClient.api.getFollowers(
                            apiKey = BuildConfig.SUPABASE_ANON_KEY,
                            auth = token,
                            followerIdEq = "eq.$uid"
                        )
                        if (followingRes.isSuccessful) {
                            followingRes.body().orEmpty().forEach { f ->
                                val targetId = f.following_id
                                if (targetId.isNotBlank() && targetId != uid && !searchUserIds.contains(targetId)) {
                                    eligibleIds.add(targetId)
                                    sourceMap.getOrPut(targetId) { mutableSetOf() }.add(ContactSource.FOLLOWING)
                                    if (!contactMap.containsKey(targetId)) {
                                        contactMap[targetId] = EligibleContact(
                                            userId = targetId,
                                            displayName = "Learner",
                                            sources = sourceMap[targetId] ?: setOf(ContactSource.FOLLOWING)
                                        )
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("CreateGroup", "Error reading following for $uid: ${e.message}")
                    }

                    // 3. Followers (people who follow current user)
                    try {
                        val followersRes = SupabaseClient.api.getFollowers(
                            apiKey = BuildConfig.SUPABASE_ANON_KEY,
                            auth = token,
                            followingIdEq = "eq.$uid"
                        )
                        if (followersRes.isSuccessful) {
                            followersRes.body().orEmpty().forEach { f ->
                                val targetId = f.follower_id
                                if (targetId.isNotBlank() && targetId != uid && !searchUserIds.contains(targetId)) {
                                    eligibleIds.add(targetId)
                                    sourceMap.getOrPut(targetId) { mutableSetOf() }.add(ContactSource.FOLLOWER)
                                    if (!contactMap.containsKey(targetId)) {
                                        contactMap[targetId] = EligibleContact(
                                            userId = targetId,
                                            displayName = "Learner",
                                            sources = sourceMap[targetId] ?: setOf(ContactSource.FOLLOWER)
                                        )
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("CreateGroup", "Error reading followers for $uid: ${e.message}")
                    }
                }

                // 4. Enrich profile details from discoverable_profiles and profiles
                try {
                    val discProfilesRes = SupabaseClient.api.getDiscoverableProfiles(
                        apiKey = BuildConfig.SUPABASE_ANON_KEY,
                        auth = token,
                        options = emptyMap()
                    )
                    if (discProfilesRes.isSuccessful) {
                        discProfilesRes.body().orEmpty().forEach { p ->
                            val pUid = p.auth_user_id
                            if (eligibleIds.contains(pUid)) {
                                val name = p.profile_name?.ifBlank { null }
                                    ?: p.username?.ifBlank { null }
                                    ?: contactMap[pUid]?.displayName
                                    ?: "Learner"
                                val avatar = p.avatar_url?.ifBlank { null }
                                    ?: contactMap[pUid]?.avatarUrl
                                val langs = formatMemberLanguages(p.native_languages, p.learning_languages)
                                val userSources = sourceMap[pUid] ?: setOf(ContactSource.FOLLOWING)
                                contactMap[pUid] = EligibleContact(
                                    userId = pUid,
                                    displayName = name,
                                    username = p.username,
                                    avatarUrl = avatar,
                                    languages = langs,
                                    sources = userSources
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("CreateGroup", "Error reading discoverable profiles: ${e.message}")
                }

                try {
                    val allProfilesRes = SupabaseClient.api.getAllProfiles(
                        apiKey = BuildConfig.SUPABASE_ANON_KEY,
                        auth = token
                    )
                    if (allProfilesRes.isSuccessful) {
                        allProfilesRes.body().orEmpty().forEach { p ->
                            val pUid = p.auth_user_id ?: p.id.toString()
                            if (eligibleIds.contains(pUid)) {
                                val existing = contactMap[pUid]
                                val name = p.profile_name?.ifBlank { null }
                                    ?: p.username?.ifBlank { null }
                                    ?: existing?.displayName
                                    ?: "Learner"
                                val avatar = p.avatar_url?.ifBlank { null }
                                    ?: existing?.avatarUrl
                                val langs = formatMemberLanguages(p.native_languages, p.learning_languages)
                                val userSources = sourceMap[pUid] ?: existing?.sources ?: setOf(ContactSource.FOLLOWING)
                                contactMap[pUid] = EligibleContact(
                                    userId = pUid,
                                    displayName = name,
                                    username = p.username ?: existing?.username,
                                    avatarUrl = avatar,
                                    languages = if (langs != "EN • GLOBAL") langs else (existing?.languages ?: langs),
                                    sources = userSources
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("CreateGroup", "Error reading profiles: ${e.message}")
                }

                withContext(Dispatchers.Main) {
                    eligibleContacts.clear()
                    eligibleContacts.addAll(contactMap.values.sortedBy { it.displayName.lowercase() })
                }
            } catch (e: Exception) {
                android.util.Log.e("CreateGroup", "Error loading eligible contacts", e)
            } finally {
                withContext(Dispatchers.Main) {
                    isLoadingContacts = false
                }
            }
        }
    }

    val followingCount = remember(eligibleContacts) { eligibleContacts.count { it.sources.contains(ContactSource.FOLLOWING) } }
    val followersCount = remember(eligibleContacts) { eligibleContacts.count { it.sources.contains(ContactSource.FOLLOWER) } }
    val chatsCount = remember(eligibleContacts) { eligibleContacts.count { it.sources.contains(ContactSource.CHAT) } }

    val filteredContacts = remember(eligibleContacts, searchQuery, selectedCategoryFilter) {
        var list = eligibleContacts.toList()

        // Filter by category pill
        list = when (selectedCategoryFilter) {
            "Following" -> list.filter { it.sources.contains(ContactSource.FOLLOWING) }
            "Followers" -> list.filter { it.sources.contains(ContactSource.FOLLOWER) }
            "Recent Chats" -> list.filter { it.sources.contains(ContactSource.CHAT) }
            else -> list
        }

        // Filter by search text
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.trim().lowercase()
            list = list.filter {
                it.displayName.lowercase().contains(query) ||
                    (it.username?.lowercase()?.contains(query) == true) ||
                    it.languages.lowercase().contains(query)
            }
        }
        list
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFE5E7EB))
                )
            }
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 22.dp, vertical = 6.dp)
        ) {
            // Top Navigation & Step Indicator Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (currentStep == 2) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF3F4F6))
                                .clickable { currentStep = 1 },
                            contentAlignment = Alignment.Center
                        ) {
                            GroupBackIcon(size = 16.dp, color = Color(0xFF111827))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFEF3C7))
                                .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            GroupUsersIcon(size = 20.dp, color = Color(0xFFB45309))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Column {
                        Text(
                            text = if (currentStep == 1) "Create Group Chat" else "Invite Members",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF111827)
                        )
                        Text(
                            text = if (currentStep == 1) "Step 1 of 2: Details & Photo" else "Step 2 of 2: Select connections",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    SoftCloseIcon(size = 14.dp, color = Color(0xFF6B7280))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Step Progress Bar (Warm Gold & Smooth Gray)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(GoldAccent)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (currentStep == 2) GoldAccent else Color(0xFFE5E7EB))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // PART 1: GROUP DETAILS & COVER PHOTO
            // ==========================================
            if (currentStep == 1) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Group Photo Selection
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Group Photo",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF111827)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Choose a preset cover or upload your own",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Add Custom Photo (Warm Amber Styling)
                                item {
                                    val isCustomSelected = customAvatarUri != null && selectedAvatar == customAvatarUri.toString()
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFFFFFBEB))
                                            .dashedBorder(
                                                width = 1.6.dp,
                                                color = if (isCustomSelected) Color(0xFFD97706) else Color(0xFFF59E0B),
                                                cornerRadius = 16.dp
                                            )
                                            .clickable {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (customAvatarUri != null) {
                                            AsyncImage(
                                                model = customAvatarUri,
                                                contentDescription = "Custom Group Photo",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(16.dp))
                                            )
                                        } else {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                GroupCameraIcon(size = 22.dp, color = Color(0xFFB45309))
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Add Photo",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB45309)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Presets
                                items(GROUP_AVATAR_PRESETS) { url ->
                                    val isSelected = selectedAvatar == url
                                    Box(
                                        modifier = Modifier
                                            .size(76.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .border(
                                                BorderStroke(
                                                    width = if (isSelected) 3.dp else 1.dp,
                                                    color = if (isSelected) GoldAccent else Color(0xFFE5E7EB)
                                                ),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .clickable {
                                                selectedAvatar = url
                                                customAvatarUri = null
                                            }
                                    ) {
                                        AsyncImage(
                                            model = url,
                                            contentDescription = "Group Photo Preset",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(16.dp))
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .align(Alignment.BottomEnd)
                                                    .offset(x = (-4).dp, y = (-4).dp)
                                                    .clip(CircleShape)
                                                    .background(GoldAccent),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                GroupCheckIcon(size = 10.dp, color = Color.Black)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Group Name Input
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Group Name",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF111827)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFF9FAFB))
                                    .border(BorderStroke(1.2.dp, if (groupName.isNotBlank()) Color(0xFFFEF08A) else Color(0xFFE5E7EB)), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 16.dp),
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
                                    onValueChange = { if (it.length <= 60) groupName = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        color = Color(0xFF111827),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    cursorBrush = SolidColor(Color.Black),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Text(
                                text = "${groupName.length}/60",
                                fontSize = 11.5.sp,
                                color = Color(0xFF9CA3AF),
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .padding(top = 4.dp)
                            )
                        }
                    }

                    // Group Description
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Description (Optional)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF111827)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(88.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFF9FAFB))
                                    .border(BorderStroke(1.2.dp, Color(0xFFE5E7EB)), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                contentAlignment = Alignment.TopStart
                            ) {
                                if (groupDescription.isEmpty()) {
                                    Text(
                                        text = "What is this group about?",
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 13.5.sp
                                    )
                                }
                                BasicTextField(
                                    value = groupDescription,
                                    onValueChange = { if (it.length <= 200) groupDescription = it },
                                    textStyle = TextStyle(
                                        color = Color(0xFF111827),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Normal
                                    ),
                                    cursorBrush = SolidColor(Color.Black),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Text(
                                text = "${groupDescription.length}/200",
                                fontSize = 11.5.sp,
                                color = Color(0xFF9CA3AF),
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .padding(top = 4.dp)
                            )
                        }
                    }

                    // End-to-End Encryption Architecture Banner
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GroupE2EEShieldIcon(size = 20.dp, color = Color(0xFF16A34A))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "End-to-End Encrypted Group",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                    Text(
                                        text = "All chats and shared media are protected by local encryption.",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF166534)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Next Step Button (Vibrant FunkyTalk Gold)
                Button(
                    onClick = {
                        if (groupName.trim().isBlank()) {
                            Toast.makeText(context, "Please enter a group name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        currentStep = 2
                    },
                    enabled = groupName.trim().isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFFFEF3C7),
                        disabledContentColor = Color(0xFF9CA3AF)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = "Next: Add Members",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
            }

            // ==========================================
            // PART 2: INVITE ELIGIBLE MEMBERS
            // ==========================================
            if (currentStep == 2) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Eligibility Information Note
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, Color(0xFFFEF08A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GroupTinyLockIcon(size = 15.dp, color = Color(0xFFB45309))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "You can invite users you follow, your followers, or people you've chatted with.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    // Category Filter Pills: All, Following, Followers, Recent Chats
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val filters = listOf(
                                "All" to eligibleContacts.size,
                                "Following" to followingCount,
                                "Followers" to followersCount,
                                "Recent Chats" to chatsCount
                            )
                            items(filters) { (title, count) ->
                                val isSelected = selectedCategoryFilter == title
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(if (isSelected) Color(0xFFFFC529) else Color(0xFFF3F4F6))
                                        .clickable { selectedCategoryFilter = title }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = "$title ($count)",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                        color = if (isSelected) Color.Black else Color(0xFF4B5563)
                                    )
                                }
                            }
                        }
                    }

                    // Search Bar
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF9FAFB))
                                .border(BorderStroke(1.dp, Color(0xFFE5E7EB)), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                GroupSearchIcon(size = 17.dp, color = Color(0xFF9CA3AF))
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search name or username...",
                                            color = Color(0xFF9CA3AF),
                                            fontSize = 13.5.sp
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        singleLine = true,
                                        textStyle = TextStyle(color = Color(0xFF111827), fontSize = 13.5.sp, fontWeight = FontWeight.Medium),
                                        cursorBrush = SolidColor(Color.Black),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (searchQuery.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE5E7EB))
                                            .clickable { searchQuery = "" },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        GroupCloseIcon(size = 9.dp, color = Color(0xFF4B5563))
                                    }
                                }
                            }
                        }
                    }

                    // Selected Members Chip Carousel (if any selected)
                    if (selectedMemberIds.isNotEmpty()) {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Selected Members",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF111827)
                                    )
                                    Text(
                                        text = "${selectedMemberIds.size} selected",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFD97706)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(selectedMemberIds) { id ->
                                        val contact = eligibleContacts.find { it.userId == id }
                                        Surface(
                                            shape = RoundedCornerShape(50.dp),
                                            color = Color(0xFFFFFBEB),
                                            border = BorderStroke(1.dp, Color(0xFFFEF08A))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(start = 4.dp, top = 3.dp, bottom = 3.dp, end = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (!contact?.avatarUrl.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = contact!!.avatarUrl,
                                                        contentDescription = contact.displayName,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFFD97706)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = (contact?.displayName ?: "U").take(1).uppercase(),
                                                            fontSize = 11.sp,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Black
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = contact?.displayName ?: "User",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF92400E)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(16.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFFDE68A))
                                                        .clickable { selectedMemberIds.remove(id) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    GroupCloseIcon(size = 7.dp, color = Color(0xFF92400E))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Contacts List Container
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFFF9FAFB),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isLoadingContacts) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(26.dp),
                                        color = GoldAccent,
                                        strokeWidth = 2.5.dp
                                    )
                                }
                            } else if (filteredContacts.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 28.dp, horizontal = 18.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        GroupUsersIcon(size = 32.dp, color = Color(0xFF9CA3AF))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = if (searchQuery.isNotBlank()) {
                                                "No connections match \"$searchQuery\""
                                            } else if (selectedCategoryFilter != "All") {
                                                "No contacts found in $selectedCategoryFilter."
                                            } else {
                                                "You can only invite people you follow, your followers, or people you've chatted with. Start a chat or follow users to invite them!"
                                            },
                                            fontSize = 13.sp,
                                            color = Color(0xFF6B7280),
                                            textAlign = TextAlign.Center,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    filteredContacts.forEachIndexed { index, contact ->
                                        val isSelected = selectedMemberIds.contains(contact.userId)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (isSelected) {
                                                        selectedMemberIds.remove(contact.userId)
                                                    } else {
                                                        selectedMemberIds.add(contact.userId)
                                                    }
                                                }
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // User Avatar
                                            if (!contact.avatarUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = contact.avatarUrl,
                                                    contentDescription = contact.displayName,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .clip(CircleShape)
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF1E293B)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = contact.displayName.take(1).uppercase(),
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 16.sp,
                                                        color = Color.White
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(12.dp))

                                            // Name & Badges
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = contact.displayName,
                                                    fontSize = 14.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF111827),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
                                                    Text(
                                                        text = contact.languages,
                                                        fontSize = 11.5.sp,
                                                        color = Color(0xFF6B7280),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    if (contact.sources.contains(ContactSource.FOLLOWING)) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color(0xFFDCFCE7))
                                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                                        ) {
                                                            Text(
                                                                text = "Following",
                                                                fontSize = 9.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF15803D)
                                                            )
                                                        }
                                                    }
                                                    if (contact.sources.contains(ContactSource.FOLLOWER)) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color(0xFFFEF3C7))
                                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                                        ) {
                                                            Text(
                                                                text = "Follower",
                                                                fontSize = 9.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFFB45309)
                                                            )
                                                        }
                                                    }
                                                    if (contact.sources.contains(ContactSource.CHAT)) {
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(Color(0xFFE0E7FF))
                                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                                        ) {
                                                            Text(
                                                                text = "In Chat",
                                                                fontSize = 9.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF4338CA)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Selection Circle with Custom Checkmark
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(GoldAccent),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    GroupCheckIcon(size = 12.dp, color = Color.Black)
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .border(BorderStroke(1.8.dp, Color(0xFFD1D5DB)), CircleShape)
                                                )
                                            }
                                        }

                                        if (index < filteredContacts.lastIndex) {
                                            HorizontalDivider(
                                                color = Color(0xFFF3F4F6),
                                                thickness = 1.dp,
                                                modifier = Modifier.padding(start = 68.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Create CTA Button
                Button(
                    onClick = {
                        if (groupName.isBlank()) {
                            Toast.makeText(context, "Please enter a group name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isCreating = true

                        val selectedNames = eligibleContacts
                            .filter { selectedMemberIds.contains(it.userId) }
                            .map { it.displayName }

                        scope.launch {
                            val (customBytes, customMime) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                val u = customAvatarUri
                                if (u != null) {
                                    val b = context.contentResolver.openInputStream(u)?.use { it.readBytes() }
                                    val m = context.contentResolver.getType(u) ?: "image/jpeg"
                                    b to m
                                } else {
                                    null to "image/jpeg"
                                }
                            }

                            val serverRes = GroupChatRepository.createGroup(
                                context = context,
                                name = groupName.trim(),
                                memberUserIds = selectedMemberIds.toList(),
                                description = groupDescription.trim().takeIf { it.isNotBlank() },
                                avatarUrl = if (customBytes == null) selectedAvatar else null,
                                localAvatarBytes = customBytes,
                                localAvatarMime = customMime
                            )
                            isCreating = false

                            serverRes.onSuccess { realId ->
                                val created = ChatPreferencesManager.createGroup(
                                    context = context,
                                    name = groupName.trim(),
                                    description = groupDescription.trim(),
                                    memberNames = selectedNames,
                                    avatarUrl = selectedAvatar,
                                    groupId = realId
                                )
                                Toast.makeText(context, "Group \"${groupName.trim()}\" created!", Toast.LENGTH_SHORT).show()
                                onGroupCreated(created)
                                onDismiss()
                            }.onFailure { err ->
                                Toast.makeText(
                                    context,
                                    err.message ?: "Failed to create group",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    enabled = groupName.isNotBlank() && !isCreating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFFFEF3C7),
                        disabledContentColor = Color(0xFF9CA3AF)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Creating Group...", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GroupUsersIcon(size = 20.dp, color = Color.Black)
                            Text(
                                text = if (selectedMemberIds.isNotEmpty()) "Create Group (${selectedMemberIds.size} Members)" else "Create Group Chat",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
