package com.example.ui.screens.home.dialogs

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.BuildConfig
import com.example.DiscoverableProfile
import com.example.MyModerationStatus
import com.example.ModerationManager
import com.example.ReportMessageSnapshot
import com.example.ReportSpeakerLogItem
import com.example.RoomReportItem
import com.example.MomentDto
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.FeedbackPostDto
import com.example.FeedbackReplyDto
import com.example.SupportChatManager
import com.example.UserProfile
import com.example.repository.FeedbackRepository
import com.example.repository.MomentsRepository
import com.example.ui.components.*
import com.example.ui.screens.FeedbackKindChip
import com.example.ui.screens.FeedbackStatusChip
import com.example.ui.screens.home.models.DarkText
import com.example.ui.screens.home.models.GraySubtitle
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.LocalFunkyColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Stable
class ReportAudioPlayerState(
    private val context: android.content.Context,
    private val scope: CoroutineScope
) {
    var mediaPlayer by mutableStateOf<MediaPlayer?>(null)
    var currentUrl by mutableStateOf<String?>(null)
    var isPlaying by mutableStateOf(false)
    var isBuffering by mutableStateOf(false)
    var currentPositionMs by mutableIntStateOf(0)
    var durationMs by mutableIntStateOf(0)
    var playbackError by mutableStateOf<String?>(null)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun togglePlay(url: String) {
        if (currentUrl == url && mediaPlayer != null) {
            if (isPlaying) {
                try {
                    mediaPlayer?.pause()
                } catch (_: Exception) {}
                isPlaying = false
            } else {
                try {
                    mediaPlayer?.start()
                    isPlaying = true
                } catch (e: Exception) {
                    stop()
                    startStreaming(url)
                }
            }
            return
        }

        stop()
        startStreaming(url)
    }

    private fun startStreaming(url: String) {
        currentUrl = url
        isBuffering = true
        isPlaying = false
        playbackError = null
        currentPositionMs = 0
        durationMs = 0

        scope.launch(Dispatchers.IO) {
            try {
                // Verify admin authorization prior to accessing evidence
                val isAdmin = ModerationManager.checkIsAdmin(context)
                if (!isAdmin) {
                    withContext(Dispatchers.Main) {
                        isBuffering = false
                        isPlaying = false
                        playbackError = "Access denied"
                        Toast.makeText(context, "Access Restricted: Administrator role required to review audio evidence.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val cacheDir = context.cacheDir.resolve("mod_audio_cache")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val safeFileName = "clip_" + abs(url.hashCode().toLong()) + ".mp3"
                val localAudioFile = File(cacheDir, safeFileName)

                // If cached file exists, validate it is not a corrupted/JSON error payload
                if (localAudioFile.exists()) {
                    if (localAudioFile.length() < 512L || isErrorPayload(localAudioFile)) {
                        localAudioFile.delete()
                    }
                }

                if (!localAudioFile.exists() || localAudioFile.length() <= 0L) {
                    val downloadSuccess = com.example.SupabaseMediaResolver.downloadEvidenceFile(context, url, localAudioFile)
                    if (!downloadSuccess) {
                        localAudioFile.delete()
                        withContext(Dispatchers.Main) {
                            isBuffering = false
                            isPlaying = false
                            playbackError = "Audio evidence unavailable"
                            Toast.makeText(context, "Audio evidence is unavailable or access is restricted.", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                }

                // Final safety check: ensure the downloaded file is a valid audio file
                if (!localAudioFile.exists() || localAudioFile.length() < 512L || isErrorPayload(localAudioFile)) {
                    localAudioFile.delete()
                    withContext(Dispatchers.Main) {
                        isBuffering = false
                        isPlaying = false
                        playbackError = "Invalid audio file format"
                        Toast.makeText(context, "Audio clip format is invalid or unavailable", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    try {
                        val mp = MediaPlayer()
                        mediaPlayer = mp
                        mp.setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )

                        mp.setDataSource(localAudioFile.absolutePath)

                        mp.setOnPreparedListener { player ->
                            isBuffering = false
                            durationMs = player.duration
                            player.start()
                            isPlaying = true
                        }

                        mp.setOnCompletionListener {
                            isPlaying = false
                            currentPositionMs = 0
                        }

                        mp.setOnErrorListener { _, _, _ ->
                            isBuffering = false
                            isPlaying = false
                            playbackError = "Could not play audio clip. Please try again."
                            try {
                                localAudioFile.delete()
                            } catch (_: Exception) {}
                            true
                        }

                        mp.prepareAsync()
                    } catch (e: Exception) {
                        isBuffering = false
                        isPlaying = false
                        playbackError = com.example.util.ErrorMapper.map(e)
                        try {
                            localAudioFile.delete()
                        } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isBuffering = false
                    isPlaying = false
                    playbackError = com.example.util.ErrorMapper.map(e)
                }
            }
        }
    }

    private fun isErrorPayload(file: File): Boolean {
        return try {
            if (!file.exists() || file.length() < 64) return true
            val header = ByteArray(64)
            val read = file.inputStream().use { it.read(header) }
            if (read <= 0) return true
            val text = String(header, 0, read, Charsets.UTF_8).trim()
            text.startsWith("{") || text.startsWith("<!DOCTYPE") || text.startsWith("<html") || text.contains("NoSuchKey") || text.contains("Bucket not found")
        } catch (_: Exception) {
            false
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        currentUrl = null
        isPlaying = false
        isBuffering = false
        currentPositionMs = 0
        durationMs = 0
        playbackError = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationReviewDialog(
    onDismiss: () -> Unit,
    currentUserProfile: UserProfile? = null,
    supabaseProfiles: List<UserProfile> = emptyList(),
    initialReportId: String? = null
) {
    val context = LocalContext.current
    val funkyColors = LocalFunkyColors.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val adminUnreadSupportCount by com.example.AdminSupportManager.unreadConversationsCount.collectAsState()
    val audioPlayer = remember(scope) { ReportAudioPlayerState(context, scope) }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.stop()
        }
    }

    LaunchedEffect(audioPlayer.isPlaying) {
        while (audioPlayer.isPlaying) {
            audioPlayer.mediaPlayer?.let { mp ->
                try {
                    if (mp.isPlaying) {
                        audioPlayer.currentPositionMs = mp.currentPosition
                        audioPlayer.durationMs = mp.duration
                    }
                } catch (_: Exception) {}
            }
            delay(250)
        }
    }

    var activeReportDetailId by remember { mutableStateOf<String?>(initialReportId) }
    LaunchedEffect(initialReportId) {
        if (initialReportId != null) {
            activeReportDetailId = initialReportId
        }
    }

    var reportsList by remember { mutableStateOf<List<RoomReportItem>>(emptyList()) }
    var activePenaltiesList by remember { mutableStateOf<List<MyModerationStatus>>(emptyList()) }
    var selectedAdminTab by remember { mutableIntStateOf(0) } // 0: Reports, 1: Active Restrictions, 2: Support Inbox
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
    var actionDialogReport by remember { mutableStateOf<RoomReportItem?>(null) }

    // Revocation state
    var revokeTargetUserId by remember { mutableStateOf<String?>(null) }
    var showRevokeConfirmDialog by remember { mutableStateOf(false) }

    fun fetchAllData() {
        isLoading = true
        errorMessage = null
        scope.launch(Dispatchers.IO) {
            try {
                // Ensure active Supabase session
                SupabaseSessionManager.ensureActiveSession(context)
                val bearerAuth = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                if (bearerAuth == null) {
                    withContext(Dispatchers.Main) {
                        activePenaltiesList = emptyList()
                        errorMessage = "Authentication error: Active session required to view incident reports."
                        isLoading = false
                    }
                    return@launch
                }

                // Check admin authorization via user_roles table
                val isAdmin = ModerationManager.checkIsAdmin(context)
                if (!isAdmin) {
                    withContext(Dispatchers.Main) {
                        activePenaltiesList = emptyList()
                        errorMessage = "Access Restricted: Administrator permissions required in public.user_roles."
                        isLoading = false
                    }
                    return@launch
                }

                val penalties = ModerationManager.fetchActiveModerationPenalties(context)

                withContext(Dispatchers.Main) {
                    activePenaltiesList = penalties
                    errorMessage = null
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    activePenaltiesList = emptyList()
                    errorMessage = com.example.util.ErrorMapper.map(e)
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchAllData()
    }

    // Filtered reports
    val filteredReports = remember(reportsList, searchQuery, selectedFilter) {
        reportsList.filter { report ->
            val matchesFilter = when (selectedFilter) {
                "Pending" -> (report.status ?: "pending").equals("pending", ignoreCase = true)
                "Solved" -> (report.status ?: "").equals("solved", ignoreCase = true) || (report.status ?: "").equals("resolved", ignoreCase = true)
                "Urgent" -> (report.category ?: "").contains("Abuse", ignoreCase = true) ||
                        (report.category ?: "").contains("Harassment", ignoreCase = true)
                "Profile" -> (report.context_type ?: "room").equals("profile", ignoreCase = true)
                "DM" -> (report.context_type ?: "room").equals("dm", ignoreCase = true)
                "Moment" -> (report.context_type ?: "room").equals("moment", ignoreCase = true)
                "Room" -> (report.context_type ?: "room").equals("room", ignoreCase = true)
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                (report.room_id?.lowercase()?.contains(q) == true) ||
                        (report.reporter_id?.lowercase()?.contains(q) == true) ||
                        (report.reported_user_id?.lowercase()?.contains(q) == true) ||
                        (report.category?.lowercase()?.contains(q) == true) ||
                        (report.reason?.lowercase()?.contains(q) == true) ||
                        (report.context_type?.lowercase()?.contains(q) == true) ||
                        (report.additional_details?.lowercase()?.contains(q) == true)
            }
            matchesFilter && matchesSearch
        }
    }

    val pendingCount = reportsList.count { (it.status ?: "pending").equals("pending", ignoreCase = true) }
    val solvedCount = reportsList.count { (it.status ?: "").equals("solved", ignoreCase = true) || (it.status ?: "").equals("resolved", ignoreCase = true) }
    val urgentCount = reportsList.count { (it.category ?: "").contains("Abuse", ignoreCase = true) || (it.category ?: "").contains("Harassment", ignoreCase = true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 12.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.94f)
                    .clip(RoundedCornerShape(28.dp)),
                colors = CardDefaults.cardColors(containerColor = funkyColors.bg),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar
                    Surface(
                        color = funkyColors.surface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(if (funkyColors.isDark) funkyColors.mango.copy(alpha = 0.16f) else Color(0xFFFEF3C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SoftShieldIcon(size = 22.dp, color = funkyColors.mango)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Moderation Center",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp,
                                            color = funkyColors.ink
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (funkyColors.isDark) funkyColors.ok.copy(alpha = 0.18f) else Color(0xFFD1FAE5))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "ADMIN",
                                                color = if (funkyColors.isDark) Color(0xFF6EE7B7) else Color(0xFF065F46),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Review incident reports & enforce safety",
                                        color = funkyColors.mute,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { fetchAllData() },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(funkyColors.soft)
                                ) {
                                    SoftRefreshIcon(size = 18.dp, color = funkyColors.ink)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(funkyColors.soft)
                                        .testTag("close_moderation_dialog")
                                ) {
                                    SoftCloseIcon(size = 16.dp, color = funkyColors.ink)
                                }
                            }
                        }
                    }

                    // Admin Navigation Tabs
                    PrimaryScrollableTabRow(
                        selectedTabIndex = selectedAdminTab,
                        containerColor = funkyColors.surface,
                        contentColor = funkyColors.mango,
                        edgePadding = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedAdminTab == 0,
                            onClick = { selectedAdminTab = 0 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Soft3DRulesActionIcon(modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Report Queue", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        )
                        Tab(
                            selected = selectedAdminTab == 1,
                            onClick = { selectedAdminTab = 1 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SoftBlockIcon(size = 16.dp, color = funkyColors.danger)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Active Penalties (${activePenaltiesList.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        )
                        Tab(
                            selected = selectedAdminTab == 2,
                            onClick = { selectedAdminTab = 2 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SoftHeadsetIcon(size = 16.dp, color = Color(0xFF6366F1))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Support Inbox", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    if (adminUnreadSupportCount > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(funkyColors.danger)
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "$adminUnreadSupportCount",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }
                            }
                        )
                        Tab(
                            selected = selectedAdminTab == 3,
                            onClick = { selectedAdminTab = 3 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SoftStarIcon(size = 16.dp, color = funkyColors.mango)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Badges", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        )
                        Tab(
                            selected = selectedAdminTab == 4,
                            onClick = { selectedAdminTab = 4 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SoftDocumentIcon(size = 16.dp, color = Color(0xFF10B981))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Feedback Inbox", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        )
                    }

                    HorizontalDivider(color = funkyColors.line, thickness = 1.dp)

                    if (selectedAdminTab == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        ) {
                            AdminReportQueueView(
                                onReportClick = { reportId ->
                                    activeReportDetailId = reportId
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else if (selectedAdminTab == 1) {
                        // Active Restrictions Tab
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    text = "Active Moderation Restrictions (${activePenaltiesList.size})",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = funkyColors.ink
                                )
                                Text(
                                    text = "Manage and revoke active warnings, suspensions, or mutes across FunkyTalk.",
                                    fontSize = 12.sp,
                                    color = funkyColors.mute
                                )
                            }

                            if (isLoading) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = funkyColors.mango, modifier = Modifier.size(32.dp))
                                    }
                                }
                            } else if (activePenaltiesList.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 20.dp),
                                        colors = CardDefaults.cardColors(containerColor = funkyColors.surface),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, funkyColors.line)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(CircleShape)
                                                    .background(funkyColors.soft),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                SoftShieldIcon(size = 28.dp, color = funkyColors.ok)
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "No Active Restrictions",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = funkyColors.ink
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "There are currently no users with active warning, suspend, or mute actions.",
                                                fontSize = 12.sp,
                                                color = funkyColors.mute
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(activePenaltiesList, key = { it.user_id ?: UUID.randomUUID().toString() }) { penalty ->
                                    val actionType = penalty.action_type?.lowercase() ?: "action"
                                    val badgeColor = when (actionType) {
                                        "suspend" -> funkyColors.danger
                                        "warning" -> funkyColors.mango
                                        "mute" -> if (funkyColors.isDark) Color(0xFFC4B5FD) else Color(0xFF7C3AED)
                                        else -> funkyColors.mute
                                    }
                                    val badgeBg = when (actionType) {
                                        "suspend" -> if (funkyColors.isDark) funkyColors.danger.copy(alpha = 0.16f) else Color(0xFFFEE2E2)
                                        "warning" -> if (funkyColors.isDark) funkyColors.mango.copy(alpha = 0.16f) else Color(0xFFFEF3C7)
                                        "mute" -> if (funkyColors.isDark) Color(0xFF7C3AED).copy(alpha = 0.2f) else Color(0xFFEDE9FE)
                                        else -> funkyColors.soft
                                    }

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = funkyColors.surface),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, funkyColors.line)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(badgeBg)
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = actionType.uppercase(),
                                                        color = badgeColor,
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 11.sp
                                                    )
                                                }

                                                Button(
                                                    onClick = {
                                                        revokeTargetUserId = penalty.user_id
                                                        showRevokeConfirmDialog = true
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = funkyColors.ok, contentColor = Color.White),
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    SoftSwapIcon(size = 14.dp, color = Color.White)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Revoke Action", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            val linkBlue = if (funkyColors.isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable {
                                                    penalty.user_id?.let {
                                                        clipboardManager.setText(AnnotatedString(it))
                                                        Toast.makeText(context, "User ID copied", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            ) {
                                                Text(
                                                    text = "User UUID: ${penalty.user_id ?: "Unknown"}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = linkBlue,
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                SoftCopyIcon(size = 12.dp, color = linkBlue)
                                            }

                                            if (!penalty.reason.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Reason: ${penalty.reason}",
                                                    fontSize = 12.sp,
                                                    color = funkyColors.ink
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (penalty.expires_at != null) "Expires: ${penalty.expires_at}" else "Expires: Indefinite / Manual Revoke",
                                                fontSize = 11.sp,
                                                color = funkyColors.mute
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (selectedAdminTab == 2) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        ) {
                            AdminSupportInboxView(
                                supabaseProfiles = supabaseProfiles,
                                onOpenReport = { reportId ->
                                    selectedAdminTab = 0
                                    searchQuery = reportId
                                }
                            )
                        }
                    } else if (selectedAdminTab == 3) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        ) {
                            AdminBadgesManagementView(
                                supabaseProfiles = supabaseProfiles,
                                onBadgeModified = { fetchAllData() }
                            )
                        }
                    } else if (selectedAdminTab == 4) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        ) {
                            AdminFeedbackInboxView()
                        }
                    }
                }
            }
        }
    }

    // Modal: Revoke Confirmation Dialog
    if (showRevokeConfirmDialog && revokeTargetUserId != null) {
        val targetUid = revokeTargetUserId!!
        RevokeModerationConfirmDialog(
            targetUserId = targetUid,
            onDismiss = {
                showRevokeConfirmDialog = false
                revokeTargetUserId = null
            },
            onRevoked = {
                showRevokeConfirmDialog = false
                revokeTargetUserId = null
                fetchAllData()
            }
        )
    }

    // Modal: Full Screenshot Preview
    if (previewImageUrl != null) {
        ReportScreenshotPreviewModal(
            imageUrl = previewImageUrl!!,
            onDismiss = { previewImageUrl = null }
        )
    }

    // Modal: Take Action Dialog
    if (actionDialogReport != null) {
        val rep = actionDialogReport!!
        AdminModerationActionDialog(
            report = rep,
            onDismiss = { actionDialogReport = null },
            onActionApplied = {
                actionDialogReport = null
                fetchAllData()
            }
        )
    }

    // Modal: Admin Report Detail Dialog
    if (activeReportDetailId != null) {
        AdminReportDetailDialog(
            reportId = activeReportDetailId!!,
            onDismiss = {
                activeReportDetailId = null
                ModerationManager.pendingReportIdToOpen = null
            },
            onReportUpdated = {
                fetchAllData()
            }
        )
    }
}

@Composable
fun RevokeModerationConfirmDialog(
    targetUserId: String,
    onDismiss: () -> Unit,
    onRevoked: () -> Unit
) {
    val context = LocalContext.current
    val funkyColors = LocalFunkyColors.current
    val scope = rememberCoroutineScope()
    var reasonInput by remember { mutableStateOf("") }
    var isRevoking by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isRevoking) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = funkyColors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (funkyColors.isDark) funkyColors.ok.copy(alpha = 0.16f) else Color(0xFFECFDF5)),
                        contentAlignment = Alignment.Center
                    ) {
                        SoftSwapIcon(size = 20.dp, color = funkyColors.ok)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Revoke Moderation Action", fontWeight = FontWeight.Black, fontSize = 16.sp, color = funkyColors.ink)
                        Text("Lift warning, suspension, or mute", fontSize = 12.sp, color = funkyColors.mute)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = funkyColors.soft),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, funkyColors.line),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Target User UUID:", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = funkyColors.mute)
                        Text(
                            text = targetUserId,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (funkyColors.isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Revocation Reason:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = funkyColors.ink)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = { reasonInput = it },
                    placeholder = { Text("Reason for revoking (e.g., appeal granted, resolved)...", fontSize = 12.sp, color = funkyColors.mute) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = funkyColors.mango,
                        unfocusedBorderColor = funkyColors.line,
                        focusedTextColor = funkyColors.ink,
                        unfocusedTextColor = funkyColors.ink
                    ),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, funkyColors.line),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Cancel", color = funkyColors.mute, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            isRevoking = true
                            scope.launch {
                                val result = ModerationManager.revokeModerationAction(
                                    context = context,
                                    targetUserId = targetUserId,
                                    reason = reasonInput.ifBlank { "Revoked by administrator" }
                                )
                                isRevoking = false
                                result.onSuccess { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    ModerationManager.fetchMyModerationStatus(context)
                                    onRevoked()
                                }.onFailure { e ->
                                    Toast.makeText(context, com.example.util.ErrorMapper.map(e), Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isRevoking,
                        colors = ButtonDefaults.buttonColors(containerColor = funkyColors.ok, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.3f).height(44.dp)
                    ) {
                        if (isRevoking) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Confirm Revoke", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminModerationActionDialog(
    report: RoomReportItem,
    onDismiss: () -> Unit,
    onActionApplied: () -> Unit
) {
    val context = LocalContext.current
    val funkyColors = LocalFunkyColors.current
    val scope = rememberCoroutineScope()

    // Parse offenders from speaker_log or reported_user_id
    val speakerLog = remember(report) { report.speaker_log ?: emptyList() }
    val validTargets = remember(speakerLog) {
        speakerLog.filter { SupabaseSessionManager.isValidUuid(it.user_id) }
    }

    val prefilledTarget = remember(report) {
        report.reported_user_id?.takeIf { SupabaseSessionManager.isValidUuid(it) }
    }

    var manualUserIdInput by remember { mutableStateOf("") }
    var selectedTargetUserId by remember(validTargets, report) {
        val nonReporter = validTargets.firstOrNull { it.user_id != report.reporter_id }?.user_id
        mutableStateOf(prefilledTarget ?: nonReporter ?: validTargets.firstOrNull()?.user_id ?: "")
    }

    val effectiveTargetUserId = remember(manualUserIdInput, selectedTargetUserId) {
        if (manualUserIdInput.isNotBlank()) manualUserIdInput.trim() else selectedTargetUserId.trim()
    }

    var actionType by remember { mutableStateOf("suspend") } // "suspend", "warning", "mute"
    var durationHours by remember { mutableIntStateOf(24) } // 1..90 for suspend
    var reasonInput by remember { mutableStateOf(report.reason ?: "") }
    var isSubmitting by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = funkyColors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (funkyColors.isDark) funkyColors.danger.copy(alpha = 0.16f) else Color(0xFFFEF2F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        SoftGavelIcon(size = 20.dp, color = funkyColors.danger)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Apply Moderation Action", fontWeight = FontWeight.Black, fontSize = 17.sp, color = funkyColors.ink)
                        val contextLabel = when (report.context_type?.lowercase()) {
                            "profile" -> "Profile Incident"
                            "dm" -> "Direct Message Incident"
                            "moment" -> "Moment Post Incident"
                            else -> "Room #${report.room_id?.take(8) ?: "N/A"}"
                        }
                        Text(contextLabel, fontSize = 12.sp, color = funkyColors.mute)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Target Offender Selection
                Text("Target Offender:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = funkyColors.ink)
                Spacer(modifier = Modifier.height(6.dp))

                val linkBlue = if (funkyColors.isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                if (!prefilledTarget.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (funkyColors.isDark) Color(0xFF3B82F6).copy(alpha = 0.15f) else Color(0xFFEFF6FF),
                        border = BorderStroke(1.5.dp, Color(0xFF3B82F6)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTargetUserId = prefilledTarget }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (funkyColors.isDark) Color(0xFF3B82F6).copy(alpha = 0.25f) else Color(0xFFDBEAFE)),
                                contentAlignment = Alignment.Center
                            ) {
                                SoftPersonIcon(size = 18.dp, color = linkBlue)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Reported Offender", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = linkBlue)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF2563EB))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text("AUTO-SELECTED", color = Color.White, fontSize = 8.5.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                Text(prefilledTarget, fontSize = 12.sp, color = funkyColors.ink, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (validTargets.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (funkyColors.isDark) funkyColors.mango.copy(alpha = 0.14f) else Color(0xFFFFFBEB)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (funkyColors.isDark) funkyColors.mango.copy(alpha = 0.4f) else Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SoftWarningIcon(size = 18.dp, color = funkyColors.mango)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "No reported user UUID attached. Please enter the target UUID below.",
                                fontSize = 12.sp,
                                color = if (funkyColors.isDark) funkyColors.mango else Color(0xFF92400E)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(validTargets) { item ->
                            val isSelected = (selectedTargetUserId == item.user_id)
                            Surface(
                                onClick = { selectedTargetUserId = item.user_id },
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    isSelected && funkyColors.isDark -> Color(0xFF3B82F6).copy(alpha = 0.16f)
                                    isSelected -> Color(0xFFEFF6FF)
                                    else -> funkyColors.soft
                                },
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF3B82F6) else funkyColors.line),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedTargetUserId = item.user_id },
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = item.name.ifBlank { "User" },
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = funkyColors.ink
                                                )
                                                if (item.user_id == report.reporter_id) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "(Reporter)",
                                                        color = funkyColors.danger,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Role: ${item.role} • UUID: ${item.user_id.take(8)}...",
                                                fontSize = 11.sp,
                                                color = funkyColors.mute
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Manual UUID entry option
                OutlinedTextField(
                    value = manualUserIdInput,
                    onValueChange = { manualUserIdInput = it },
                    label = { Text("Or Enter Target UUID Directly", fontSize = 11.sp) },
                    placeholder = { Text("e.g. 550e8400-e29b-41d4-a716-446655440000", fontSize = 11.sp) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 12.sp, color = funkyColors.ink),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (effectiveTargetUserId.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Target ID: $effectiveTargetUserId",
                        fontSize = 11.sp,
                        color = linkBlue,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Type Selector
                Text("Enforcement Type:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = funkyColors.ink)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "suspend" to "Suspend",
                        "warning" to "Warning",
                        "mute" to "Mute"
                    ).forEach { (key, label) ->
                        val isSelected = (actionType == key)
                        Surface(
                            onClick = { actionType = key },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) funkyColors.ink else funkyColors.soft,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    color = if (isSelected) funkyColors.bg else funkyColors.ink,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Details / Duration info
                when (actionType) {
                    "suspend" -> {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Suspension Duration:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = funkyColors.ink)
                                Text("$durationHours hours", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = funkyColors.danger)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Presets
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(1, 6, 12, 24, 48, 90).forEach { hours ->
                                    val isSelected = (durationHours == hours)
                                    Surface(
                                        onClick = { durationHours = hours },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) funkyColors.danger else funkyColors.soft,
                                        modifier = Modifier.weight(1f).height(30.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${hours}h",
                                                color = if (isSelected) Color.White else funkyColors.ink,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Blocks voice room access and messaging (1 to 90 hours).", fontSize = 11.sp, color = funkyColors.mute)
                        }
                    }
                    "warning" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SoftWarningIcon(size = 14.dp, color = funkyColors.mango)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Fixed 1 hour penalty: Messaging disabled, user cannot send chat messages or replies.", fontSize = 11.sp, color = funkyColors.mango)
                        }
                    }
                    "mute" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SoftMuteIcon(size = 14.dp, color = funkyColors.mute)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Indefinite Mute: User outgoing messaging is blocked until revoked.", fontSize = 11.sp, color = funkyColors.mute)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Reason input
                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = { reasonInput = it },
                    placeholder = { Text("Reason for moderation...", fontSize = 12.sp, color = funkyColors.mute) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = funkyColors.mango,
                        unfocusedBorderColor = funkyColors.line,
                        focusedTextColor = funkyColors.ink,
                        unfocusedTextColor = funkyColors.ink
                    ),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Submit Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, funkyColors.line),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Cancel", color = funkyColors.mute, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (effectiveTargetUserId.isBlank()) {
                                Toast.makeText(context, "Please select an offender or enter a target UUID.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (!SupabaseSessionManager.isValidUuid(effectiveTargetUserId)) {
                                Toast.makeText(context, "Invalid UUID format: '$effectiveTargetUserId'", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            isSubmitting = true
                            scope.launch {
                                val result = ModerationManager.applyModerationAction(
                                    context = context,
                                    reportId = report.id ?: "",
                                    targetUserId = effectiveTargetUserId,
                                    actionType = actionType,
                                    durationHours = if (actionType == "suspend") durationHours else null,
                                    reason = reasonInput.ifBlank { report.reason },
                                    roomId = report.room_id ?: ""
                                )

                                isSubmitting = false
                                result.onSuccess { msg ->
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    onActionApplied()
                                }.onFailure { e ->
                                    Toast.makeText(context, com.example.util.ErrorMapper.map(e), Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isSubmitting && effectiveTargetUserId.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = funkyColors.danger, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.3f).height(44.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Enforce Action", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernStatBadge(
    title: String,
    count: String,
    containerColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val funkyColors = LocalFunkyColors.current
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = funkyColors.ink
            )
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = funkyColors.mute,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ModernReportCard(
    report: RoomReportItem,
    audioPlayer: ReportAudioPlayerState,
    onViewScreenshot: (String) -> Unit,
    onCopyText: (String, String) -> Unit,
    onTakeAction: () -> Unit
) {
    val context = LocalContext.current
    val funkyColors = LocalFunkyColors.current
    val isSolved = (report.status ?: "").lowercase() in listOf("solved", "resolved", "closed")
    val isUrgent = (report.category ?: "").contains("Abuse", ignoreCase = true) || (report.category ?: "").contains("Harassment", ignoreCase = true)
    val contextType = (report.context_type ?: "room").lowercase().trim()

    var isSnapshotsExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color(0x12000000)),
        colors = CardDefaults.cardColors(containerColor = funkyColors.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isUrgent && !isSolved) funkyColors.danger.copy(alpha = 0.45f) else funkyColors.line)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Category Badge + Context Type Badge + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (badgeBg, badgeText) = when {
                        isUrgent -> (if (funkyColors.isDark) funkyColors.danger.copy(alpha = 0.16f) else Color(0xFFFEF2F2)) to funkyColors.danger
                        (report.category ?: "").contains("Spam", ignoreCase = true) -> (if (funkyColors.isDark) Color(0xFFA855F7).copy(alpha = 0.18f) else Color(0xFFF3E8FF)) to (if (funkyColors.isDark) Color(0xFFD8B4FE) else Color(0xFF7E22CE))
                        else -> (if (funkyColors.isDark) funkyColors.mango.copy(alpha = 0.16f) else Color(0xFFFFFBEB)) to funkyColors.mango
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = report.category ?: "Report",
                            color = badgeText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Context Type Pill
                    val (ctxBg, ctxText, ctxLabel) = when (contextType) {
                        "profile" -> Triple(if (funkyColors.isDark) Color(0xFF3B82F6).copy(alpha = 0.18f) else Color(0xFFEFF6FF), if (funkyColors.isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8), "Profile")
                        "dm" -> Triple(if (funkyColors.isDark) Color(0xFFA855F7).copy(alpha = 0.18f) else Color(0xFFFAF5FF), if (funkyColors.isDark) Color(0xFFD8B4FE) else Color(0xFF7E22CE), "E2EE DM")
                        "moment" -> Triple(if (funkyColors.isDark) funkyColors.ok.copy(alpha = 0.18f) else Color(0xFFECFDF5), if (funkyColors.isDark) Color(0xFF6EE7B7) else Color(0xFF047857), "Moment")
                        else -> Triple(if (funkyColors.isDark) Color(0xFFEA580C).copy(alpha = 0.18f) else Color(0xFFFFF7ED), if (funkyColors.isDark) Color(0xFFFDBA74) else Color(0xFFC2410C), "Voice Room")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ctxBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        when (contextType) {
                            "profile" -> SoftPersonIcon(size = 11.dp, color = ctxText)
                            "dm" -> SoftChatBubbleIcon(size = 11.dp, color = ctxText)
                            "moment" -> SoftImageIcon(size = 11.dp, color = ctxText)
                            else -> SoftHeadsetIcon(size = 11.dp, color = ctxText)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(ctxLabel, color = ctxText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (isUrgent && !isSolved) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(funkyColors.danger)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("URGENT", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(
                            when {
                                isSolved && funkyColors.isDark -> funkyColors.ok.copy(alpha = 0.16f)
                                isSolved -> Color(0xFFECFDF5)
                                funkyColors.isDark -> funkyColors.mango.copy(alpha = 0.16f)
                                else -> Color(0xFFFFFBEB)
                            }
                        )
                        .border(
                            1.dp,
                            if (isSolved) funkyColors.ok.copy(alpha = 0.4f) else funkyColors.mango.copy(alpha = 0.4f),
                            RoundedCornerShape(100.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isSolved) "Resolved" else "Pending",
                        color = if (isSolved) (if (funkyColors.isDark) Color(0xFF6EE7B7) else Color(0xFF047857)) else funkyColors.mango,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reason and Details
            Text(
                text = report.reason ?: "Unspecified incident",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = funkyColors.ink
            )

            if (!report.additional_details.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = report.additional_details,
                    fontSize = 13.sp,
                    color = funkyColors.mute,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // =========================================================================
            // CONTEXT-SPECIFIC DETAIL SECTION: room | profile | dm | moment
            // =========================================================================
            when (contextType) {
                "profile" -> {
                    ReportedProfileContentSection(
                        reportedUserId = report.reported_user_id ?: "",
                        reporterId = report.reporter_id ?: "",
                        onCopyText = onCopyText
                    )
                }
                "dm" -> {
                    ReportedDmContentSection(
                        reportedUserId = report.reported_user_id ?: "",
                        reporterId = report.reporter_id ?: "",
                        onCopyText = onCopyText
                    )
                }
                "moment" -> {
                    ReportedMomentContentSection(
                        momentId = report.context_id ?: "",
                        reportedUserId = report.reported_user_id ?: "",
                        reporterId = report.reporter_id ?: "",
                        onViewImage = onViewScreenshot,
                        onCopyText = onCopyText
                    )
                }
                else -> {
                    // Room context (default/unchanged)
                    ReportedRoomContentSection(
                        roomId = report.room_id ?: "",
                        reporterId = report.reporter_id ?: "",
                        onCopyText = onCopyText
                    )
                }
            }

            // Attached Media: Screenshot Evidence Thumbnail & Audio Clip Player
            val hasScreenshot = !report.screenshot_url.isNullOrBlank()
            val hasAudio = !report.audio_clip_url.isNullOrBlank()

            if (hasScreenshot || hasAudio) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (hasScreenshot) {
                        val originalScreenshotUrl = report.screenshot_url!!
                        val resolvedScreenshotUrl by produceState<String?>(initialValue = null, key1 = originalScreenshotUrl) {
                            value = com.example.SupabaseMediaResolver.resolveSignedEvidenceUrl(context, originalScreenshotUrl)
                        }
                        val effectiveScreenshotUrl = resolvedScreenshotUrl
                        val bearerToken = remember { SupabaseSessionManager.getAuthenticatedBearerToken(context) }

                        val imageRequest = remember(effectiveScreenshotUrl, bearerToken) {
                            if (!effectiveScreenshotUrl.isNullOrBlank()) {
                                ImageRequest.Builder(context)
                                    .data(effectiveScreenshotUrl)
                                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                                    .apply {
                                        if (!bearerToken.isNullOrBlank()) {
                                            addHeader("Authorization", bearerToken)
                                        }
                                    }
                                    .crossfade(true)
                                    .build()
                            } else null
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(88.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(funkyColors.soft)
                                .border(1.dp, funkyColors.line, RoundedCornerShape(12.dp))
                                .clickable {
                                    if (effectiveScreenshotUrl != null) {
                                        onViewScreenshot(effectiveScreenshotUrl)
                                    } else {
                                        Toast.makeText(context, "Resolving secure evidence...", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            SubcomposeAsyncImage(
                                model = imageRequest,
                                contentDescription = "Screenshot Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = funkyColors.mango
                                        )
                                    }
                                },
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(if (funkyColors.isDark) funkyColors.danger.copy(alpha = 0.14f) else Color(0xFFFEF2F2)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            SoftImageIcon(size = 18.dp, color = funkyColors.danger)
                                            Text("Evidence", fontSize = 10.sp, color = funkyColors.danger, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            )

                            // Overlay badge
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.25f))
                                    .padding(6.dp),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    SoftEyeIcon(size = 12.dp, visible = true, tint = Color.White)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("View Evidence", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (hasAudio) {
                        val audioUrl = report.audio_clip_url!!
                        val isThisActive = (audioPlayer.currentUrl == audioUrl)
                        val isThisPlaying = isThisActive && audioPlayer.isPlaying
                        val isThisBuffering = isThisActive && audioPlayer.isBuffering

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(88.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        isThisPlaying && funkyColors.isDark -> funkyColors.mango.copy(alpha = 0.16f)
                                        isThisPlaying -> Color(0xFFFFFBEB)
                                        else -> funkyColors.soft
                                    }
                                )
                                .border(
                                    1.dp,
                                    if (isThisPlaying) funkyColors.mango.copy(alpha = 0.55f) else funkyColors.line,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { audioPlayer.togglePlay(audioUrl) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isThisPlaying) funkyColors.mango else funkyColors.ink),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isThisBuffering) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = if (isThisPlaying) funkyColors.onMango else funkyColors.bg,
                                                strokeWidth = 2.dp
                                            )
                                        } else if (isThisPlaying) {
                                            SoftPauseIcon(size = 16.dp, color = funkyColors.onMango)
                                        } else {
                                            SoftPlayIcon(size = 16.dp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isThisBuffering) "Buffering..." else if (isThisPlaying) "Playing Evidence" else "Audio Evidence",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = funkyColors.ink,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        AudioWaveformBar(
                                            isPlaying = isThisPlaying,
                                            timeText = if (isThisActive && audioPlayer.durationMs > 0) {
                                                "${formatDurationMs(audioPlayer.currentPositionMs)} / ${formatDurationMs(audioPlayer.durationMs)}"
                                            } else {
                                                "Tap to Play"
                                            }
                                        )
                                    }
                                }

                                if (isThisActive && audioPlayer.durationMs > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = {
                                            if (audioPlayer.durationMs > 0) {
                                                (audioPlayer.currentPositionMs.toFloat() / audioPlayer.durationMs.toFloat()).coerceIn(0f, 1f)
                                            } else 0f
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = funkyColors.mango,
                                        trackColor = funkyColors.line
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Evidence: Captured Message & Speaker Snapshots (Room context only)
            if (contextType == "room") {
                val hasMessages = !report.message_snapshot.isNullOrEmpty()
                val hasSpeakers = !report.speaker_log.isNullOrEmpty()

                if (hasMessages || hasSpeakers) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        onClick = { isSnapshotsExpanded = !isSnapshotsExpanded },
                        shape = RoundedCornerShape(10.dp),
                        color = funkyColors.soft,
                        border = BorderStroke(1.dp, funkyColors.line),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Soft3DMessageStackIcon(modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Room Logs (${report.speaker_log?.size ?: 0} speakers, ${report.message_snapshot?.size ?: 0} msgs)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = funkyColors.mute
                                    )
                                }
                                SoftChevronDownIcon(
                                    size = 14.dp,
                                    color = funkyColors.mute,
                                    modifier = Modifier.rotate(if (isSnapshotsExpanded) 180f else 0f)
                                )
                            }

                            if (isSnapshotsExpanded) {
                                Spacer(modifier = Modifier.height(8.dp))

                                // Speaker log list
                                if (hasSpeakers) {
                                    Text("Speakers Captured:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = funkyColors.ink)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    report.speaker_log!!.forEach { spk ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "• ${spk.name} (${spk.role})",
                                                fontSize = 11.sp,
                                                color = funkyColors.ink
                                            )
                                            Text(
                                                text = "ID: ${spk.user_id.take(8)}...",
                                                fontSize = 10.sp,
                                                color = funkyColors.mute
                                            )
                                        }
                                    }
                                }

                                // Message snapshot
                                if (hasMessages) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Chat Messages Snapshot:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = funkyColors.ink)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    report.message_snapshot!!.takeLast(5).forEach { msg ->
                                        Text(
                                            text = "${msg.sender}: ${msg.text}",
                                            fontSize = 11.sp,
                                            color = funkyColors.mute,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Button
            Button(
                onClick = onTakeAction,
                colors = ButtonDefaults.buttonColors(containerColor = if (isSolved) Color(0xFF475569) else funkyColors.danger, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                SoftGavelIcon(size = 16.dp, color = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSolved) "Review / Update Action" else "Enforce Moderation Action",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// PROFILE REPORT SECTION: Admin view of reported profile
// -----------------------------------------------------------------------------
@Composable
private fun ReportedProfileContentSection(
    reportedUserId: String,
    reporterId: String,
    onCopyText: (String, String) -> Unit
) {
    val context = LocalContext.current
    val funkyColors = LocalFunkyColors.current
    var profileData by remember { mutableStateOf<DiscoverableProfile?>(null) }
    var isFetchingProfile by remember { mutableStateOf(true) }

    LaunchedEffect(reportedUserId) {
        if (reportedUserId.isNotBlank()) {
            isFetchingProfile = true
            withContext(Dispatchers.IO) {
                try {
                    val auth = SupabaseSessionManager.getAuthenticatedBearerToken(context) ?: BuildConfig.SUPABASE_ANON_KEY
                    val res = SupabaseClient.api.getDiscoverableProfiles(
                        auth = auth,
                        options = mapOf("id" to "eq.$reportedUserId")
                    )
                    if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                        withContext(Dispatchers.Main) {
                            profileData = res.body()!!.first()
                            isFetchingProfile = false
                        }
                    } else {
                        withContext(Dispatchers.Main) { isFetchingProfile = false }
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) { isFetchingProfile = false }
                }
            }
        } else {
            isFetchingProfile = false
        }
    }

    val linkBlue = if (funkyColors.isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // User IDs info bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (funkyColors.isDark) Color(0xFF3B82F6).copy(alpha = 0.16f) else Color(0xFFEFF6FF))
                    .clickable { onCopyText(reportedUserId, "Target User ID") }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SoftPersonIcon(size = 13.dp, color = linkBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Target: ${reportedUserId.take(8)}...", fontSize = 11.sp, color = linkBlue, fontWeight = FontWeight.SemiBold)
                    }
                    SoftCopyIcon(size = 11.dp, color = linkBlue)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(funkyColors.soft)
                    .clickable { onCopyText(reporterId, "Reporter ID") }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SoftPersonIcon(size = 13.dp, color = funkyColors.mute)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reporter: ${reporterId.take(8)}...", fontSize = 11.sp, color = funkyColors.ink, fontWeight = FontWeight.Medium)
                    }
                    SoftCopyIcon(size = 11.dp, color = funkyColors.mute)
                }
            }
        }

        // Mini Profile Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = funkyColors.soft,
            border = BorderStroke(1.dp, funkyColors.line),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isFetchingProfile) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = funkyColors.mango)
                }
            } else if (profileData != null) {
                val p = profileData!!
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatarImage(
                        avatarUrl = p.avatar_url,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = p.profile_name ?: p.username ?: "Anonymous",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = funkyColors.ink
                        )
                        Text(
                            text = "@${p.username ?: "user"}",
                            fontSize = 11.5.sp,
                            color = funkyColors.mute
                        )
                        if (!p.bio.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = p.bio,
                                fontSize = 12.sp,
                                color = funkyColors.ink.copy(alpha = 0.85f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Profile data: $reportedUserId",
                    fontSize = 12.sp,
                    color = funkyColors.mute,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// DM REPORT SECTION: E2EE Explanation and Evidence Snapshot Notice
// -----------------------------------------------------------------------------
@Composable
private fun ReportedDmContentSection(
    reportedUserId: String,
    reporterId: String,
    onCopyText: (String, String) -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    val purpleTint = if (funkyColors.isDark) Color(0xFFC084FC) else Color(0xFF7E22CE)
    val purpleBg = if (funkyColors.isDark) Color(0xFFA855F7).copy(alpha = 0.16f) else Color(0xFFFAF5FF)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(purpleBg)
                    .clickable { onCopyText(reportedUserId, "Reported Sender ID") }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SoftPersonIcon(size = 13.dp, color = purpleTint)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sender: ${reportedUserId.take(8)}...", fontSize = 11.sp, color = purpleTint, fontWeight = FontWeight.SemiBold)
                    }
                    SoftCopyIcon(size = 11.dp, color = purpleTint)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(funkyColors.soft)
                    .clickable { onCopyText(reporterId, "Reporter ID") }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SoftPersonIcon(size = 13.dp, color = funkyColors.mute)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reporter: ${reporterId.take(8)}...", fontSize = 11.sp, color = funkyColors.ink, fontWeight = FontWeight.Medium)
                    }
                    SoftCopyIcon(size = 11.dp, color = funkyColors.mute)
                }
            }
        }

        // E2EE Notice Card
        Card(
            colors = CardDefaults.cardColors(containerColor = purpleBg),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, purpleTint.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SoftLockIcon(size = 16.dp, isUnlocked = false)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "End-to-End Encrypted Message: Plaintext is never stored on servers. The decrypted snapshot was captured locally by the reporter and attached below.",
                    fontSize = 11.sp,
                    color = purpleTint,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// MOMENT REPORT SECTION: Shows the reported post text, likes, images
// -----------------------------------------------------------------------------
@Composable
private fun ReportedMomentContentSection(
    momentId: String,
    reportedUserId: String,
    reporterId: String,
    onViewImage: (String) -> Unit,
    onCopyText: (String, String) -> Unit
) {
    val context = LocalContext.current
    val funkyColors = LocalFunkyColors.current
    var momentData by remember { mutableStateOf<MomentDto?>(null) }
    var isFetchingMoment by remember { mutableStateOf(true) }

    LaunchedEffect(momentId) {
        if (momentId.isNotBlank()) {
            isFetchingMoment = true
            MomentsRepository.getMoment(context, momentId).onSuccess { dto ->
                momentData = dto
            }
            isFetchingMoment = false
        } else {
            isFetchingMoment = false
        }
    }

    val greenTint = if (funkyColors.isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
    val greenBg = if (funkyColors.isDark) funkyColors.ok.copy(alpha = 0.16f) else Color(0xFFECFDF5)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(greenBg)
                    .clickable { onCopyText(momentId, "Moment Post ID") }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SoftImageIcon(size = 13.dp, color = greenTint)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Post: ${momentId.take(8)}...", fontSize = 11.sp, color = greenTint, fontWeight = FontWeight.SemiBold)
                    }
                    SoftCopyIcon(size = 11.dp, color = greenTint)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(funkyColors.soft)
                    .clickable { onCopyText(reporterId, "Reporter ID") }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SoftPersonIcon(size = 13.dp, color = funkyColors.mute)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reporter: ${reporterId.take(8)}...", fontSize = 11.sp, color = funkyColors.ink, fontWeight = FontWeight.Medium)
                    }
                    SoftCopyIcon(size = 11.dp, color = funkyColors.mute)
                }
            }
        }

        // Moment Post Preview Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = funkyColors.soft,
            border = BorderStroke(1.dp, funkyColors.line),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isFetchingMoment) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = funkyColors.mango)
                }
            } else if (momentData != null) {
                val m = momentData!!
                Column(modifier = Modifier.padding(12.dp)) {
                    val displayText = listOfNotNull(
                        m.title?.takeIf { it.isNotBlank() },
                        m.content?.takeIf { it.isNotBlank() }
                    ).joinToString("\n").ifBlank { "[No post text]" }
                    Text(
                        text = displayText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = funkyColors.ink
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val firstMedia = m.media.firstOrNull()
                    val firstImg = firstMedia?.thumb_url?.takeIf { it.isNotBlank() } ?: firstMedia?.url
                    if (!firstImg.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(funkyColors.line)
                                .clickable { onViewImage(firstImg) }
                        ) {
                            SubcomposeAsyncImage(
                                model = firstImg,
                                contentDescription = "Reported Moment Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SoftHeartIcon(size = 14.dp, isLiked = true)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("${m.likes_count} likes", fontSize = 11.sp, color = funkyColors.mute)
                            Spacer(modifier = Modifier.width(10.dp))
                            SoftChatBubbleIcon(size = 14.dp, color = funkyColors.mute)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("${m.comments_count} comments", fontSize = 11.sp, color = funkyColors.mute)
                        }
                        Text(
                            text = "Author: ${(m.author.id.ifBlank { reportedUserId }).take(8)}...",
                            fontSize = 10.5.sp,
                            color = funkyColors.mute
                        )
                    }
                }
            } else {
                Text(
                    text = "Reported Moment Post ID: $momentId",
                    fontSize = 12.sp,
                    color = funkyColors.mute,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// ROOM REPORT SECTION: Room ID and Reporter Chips
// -----------------------------------------------------------------------------
@Composable
private fun ReportedRoomContentSection(
    roomId: String,
    reporterId: String,
    onCopyText: (String, String) -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(funkyColors.soft)
                .clickable { onCopyText(roomId, "Room ID") }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SoftGlobeIcon(size = 13.dp, color = funkyColors.mute)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Room: ${roomId.take(8)}...", fontSize = 11.sp, color = funkyColors.ink, fontWeight = FontWeight.Medium)
                }
                SoftCopyIcon(size = 11.dp, color = funkyColors.mute)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(funkyColors.soft)
                .clickable { onCopyText(reporterId, "Reporter ID") }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SoftPersonIcon(size = 13.dp, color = funkyColors.mute)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reporter: ${reporterId.take(8)}...", fontSize = 11.sp, color = funkyColors.ink, fontWeight = FontWeight.Medium)
                }
                SoftCopyIcon(size = 11.dp, color = funkyColors.mute)
            }
        }
    }
}

@Composable
private fun ReportScreenshotPreviewModal(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var reloadKey by remember { mutableIntStateOf(0) }

    val resolvedUrl by produceState<String?>(initialValue = null, key1 = imageUrl, key2 = reloadKey) {
        value = com.example.SupabaseMediaResolver.resolveSignedEvidenceUrl(context, imageUrl)
    }
    val activeUrl = resolvedUrl ?: imageUrl
    val bearerToken = remember { SupabaseSessionManager.getAuthenticatedBearerToken(context) }

    val imageRequest = remember(activeUrl, bearerToken, reloadKey) {
        ImageRequest.Builder(context)
            .data(activeUrl)
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .apply {
                if (!bearerToken.isNullOrBlank()) {
                    addHeader("Authorization", bearerToken)
                }
            }
            .crossfade(true)
            .build()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            SoftImageIcon(size = 18.dp, color = GoldPrimary)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Screenshot Evidence", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("High-resolution incident preview", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                try {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(activeUrl))
                                    context.startActivity(browserIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open external browser", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            SoftGlobeIcon(size = 18.dp, color = Color(0xFF94A3B8))
                        }

                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(activeUrl))
                                Toast.makeText(context, "Image URL copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            SoftCopyIcon(size = 16.dp, color = Color(0xFF94A3B8))
                        }

                        IconButton(onClick = onDismiss) {
                            SoftCloseIcon(size = 18.dp, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Image Viewer Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF020617))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    key(activeUrl, reloadKey) {
                        SubcomposeAsyncImage(
                            model = imageRequest,
                            contentDescription = "Full Screenshot Zoom",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                            loading = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Loading high-res evidence image...", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                }
                            },
                            error = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    SoftImageIcon(size = 36.dp, color = Color(0xFFEF4444))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Screenshot file unavailable", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Attachment might not exist on storage or was removed.", color = Color(0xFF94A3B8), fontSize = 11.sp, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = {
                                            reloadKey++
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary)
                                    ) {
                                        SoftRefreshIcon(size = 16.dp, color = Color.Black)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Retry Loading", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioWaveformBar(
    isPlaying: Boolean,
    timeText: String = "0:14"
) {
    val funkyColors = LocalFunkyColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "audio_bars")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = if (isPlaying) 14f else 4f,
        animationSpec = infiniteRepeatable(tween(300, easing = LinearEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = if (isPlaying) 18f else 8f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 5f,
        targetValue = if (isPlaying) 12f else 5f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
        label = "h3"
    )

    val barColor = if (isPlaying) funkyColors.mango else funkyColors.mute
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(top = 2.dp)
    ) {
        Box(modifier = Modifier.width(3.dp).height(h1.dp).clip(RoundedCornerShape(2.dp)).background(barColor))
        Box(modifier = Modifier.width(3.dp).height(h2.dp).clip(RoundedCornerShape(2.dp)).background(barColor))
        Box(modifier = Modifier.width(3.dp).height(h3.dp).clip(RoundedCornerShape(2.dp)).background(barColor))
        Box(modifier = Modifier.width(3.dp).height(h1.dp).clip(RoundedCornerShape(2.dp)).background(barColor))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = timeText,
            fontSize = 10.sp,
            color = if (isPlaying) funkyColors.mango else funkyColors.mute,
            fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun formatDurationMs(ms: Int): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

@Composable
fun AdminBadgesManagementView(
    initialUserId: String? = null,
    supabaseProfiles: List<UserProfile> = emptyList(),
    onBadgeModified: () -> Unit = {}
) {
    val context = LocalContext.current
    val funkyColors = LocalFunkyColors.current
    val coroutineScope = rememberCoroutineScope()

    var targetUserId by remember { mutableStateOf(initialUserId ?: "") }
    val catalogBadges = remember { mutableStateListOf<com.example.BadgeCatalogItem>() }
    val userBadges = remember { mutableStateListOf<com.example.ProfileBadge>() }
    var isLoadingCatalog by remember { mutableStateOf(false) }
    var isLoadingUserBadges by remember { mutableStateOf(false) }
    var actionInProgress by remember { mutableStateOf<String?>(null) }

    fun refreshUserBadges(uid: String) {
        if (uid.isBlank()) return
        isLoadingUserBadges = true
        coroutineScope.launch {
            try {
                val badges = com.example.BadgeManager.getBadgesForUser(uid.trim())
                userBadges.clear()
                userBadges.addAll(badges)
            } catch (e: Exception) {
                android.util.Log.e("AdminBadges", "Failed to load user badges", e)
            } finally {
                isLoadingUserBadges = false
            }
        }
    }

    LaunchedEffect(Unit) {
        isLoadingCatalog = true
        try {
            val cat = com.example.BadgeManager.fetchActiveCatalog()
            catalogBadges.clear()
            catalogBadges.addAll(cat)
        } catch (e: Exception) {
            android.util.Log.e("AdminBadges", "Failed to load catalog", e)
        } finally {
            isLoadingCatalog = false
        }
        if (targetUserId.isNotBlank()) {
            refreshUserBadges(targetUserId)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header & Description
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (funkyColors.isDark) funkyColors.mango.copy(alpha = 0.14f) else Color(0xFFFFFBEB)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (funkyColors.isDark) funkyColors.mango.copy(alpha = 0.4f) else Color(0xFFFDE68A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(funkyColors.mango),
                        contentAlignment = Alignment.Center
                    ) {
                        SoftStarIcon(size = 22.dp, color = funkyColors.onMango)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Badges & Honors Management",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (funkyColors.isDark) funkyColors.mango else Color(0xFF92400E)
                        )
                        Text(
                            text = "Manually award or revoke prestigious badges (e.g. Founder, VIP) to community members.",
                            fontSize = 12.sp,
                            color = if (funkyColors.isDark) funkyColors.ink.copy(alpha = 0.85f) else Color(0xFFB45309),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // 2. Target User Input / Picker
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = funkyColors.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, funkyColors.line),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Target User UUID / ID",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = funkyColors.ink
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = targetUserId,
                            onValueChange = { targetUserId = it },
                            placeholder = { Text("Enter user auth UUID...", fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { refreshUserBadges(targetUserId) },
                            colors = ButtonDefaults.buttonColors(containerColor = funkyColors.mango, contentColor = funkyColors.onMango),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            SoftSearchIcon(size = 14.dp, color = funkyColors.onMango)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Inspect", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Quick User Suggestion Chips
                    if (supabaseProfiles.isNotEmpty()) {
                        Text(
                            text = "Quick Select:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = funkyColors.mute
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(supabaseProfiles.take(8)) { profile ->
                                val isSelected = targetUserId == profile.uid
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .clickable {
                                            targetUserId = profile.uid
                                            refreshUserBadges(profile.uid)
                                        },
                                    color = if (isSelected) (if (funkyColors.isDark) funkyColors.mango.copy(alpha = 0.22f) else Color(0xFFFEF3C7)) else funkyColors.soft,
                                    shape = RoundedCornerShape(100.dp),
                                    border = BorderStroke(1.dp, if (isSelected) funkyColors.mango else funkyColors.line)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = profile.displayName ?: profile.username ?: profile.uid.take(6),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) funkyColors.mango else funkyColors.ink
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. User's Current Awarded Badges (with Revoke action)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = funkyColors.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, funkyColors.line),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Currently Awarded Badges (${userBadges.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.ink
                        )
                        if (isLoadingUserBadges) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = funkyColors.mango)
                        }
                    }

                    if (targetUserId.isBlank()) {
                        Text(
                            text = "Enter a user ID above to view and manage their badges.",
                            fontSize = 12.sp,
                            color = funkyColors.mute
                        )
                    } else if (userBadges.isEmpty()) {
                        Text(
                            text = "This user currently has no assigned badges.",
                            fontSize = 12.sp,
                            color = funkyColors.mute
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            userBadges.forEach { badge ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = funkyColors.soft,
                                    border = BorderStroke(1.dp, funkyColors.line),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            ProfileBadgeChip(badge = badge, size = 24.dp)
                                            Column {
                                                Text(
                                                    text = badge.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = funkyColors.ink
                                                )
                                                Text(
                                                    text = "code: ${badge.code}",
                                                    fontSize = 10.sp,
                                                    color = funkyColors.mute
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                actionInProgress = badge.code
                                                coroutineScope.launch {
                                                    val res = com.example.BadgeManager.revokeBadge(context, targetUserId.trim(), badge.code)
                                                    if (res.isSuccess) {
                                                        Toast.makeText(context, "Revoked ${badge.name}", Toast.LENGTH_SHORT).show()
                                                        refreshUserBadges(targetUserId)
                                                        onBadgeModified()
                                                    } else {
                                                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Revoke failed", Toast.LENGTH_LONG).show()
                                                    }
                                                    actionInProgress = null
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (funkyColors.isDark) funkyColors.danger.copy(alpha = 0.2f) else Color(0xFFFEE2E2),
                                                contentColor = funkyColors.danger
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            enabled = actionInProgress == null
                                        ) {
                                            Text("Revoke", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Available Catalog Badges (with Award action)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = funkyColors.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, funkyColors.line),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Available Badges in Catalog",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = funkyColors.ink
                    )

                    if (isLoadingCatalog) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = funkyColors.mango)
                    } else if (catalogBadges.isEmpty()) {
                        Text("No badges found in catalog.", fontSize = 12.sp, color = funkyColors.mute)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            catalogBadges.forEach { catBadge ->
                                val isAlreadyAwarded = userBadges.any { it.code.equals(catBadge.code, ignoreCase = true) }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isAlreadyAwarded) funkyColors.soft else if (funkyColors.isDark) funkyColors.mango.copy(alpha = 0.12f) else Color(0xFFFFFBEB).copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, if (isAlreadyAwarded) funkyColors.line else funkyColors.mango.copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            EmblemVectorBadge(badgeCode = catBadge.code, size = 26.dp)
                                            Column {
                                                Text(
                                                    text = catBadge.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = funkyColors.ink
                                                )
                                                Text(
                                                    text = catBadge.description ?: "Special community badge",
                                                    fontSize = 11.sp,
                                                    color = funkyColors.mute,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                if (targetUserId.isBlank()) {
                                                    Toast.makeText(context, "Please enter a target User UUID first", Toast.LENGTH_SHORT).show()
                                                    return@Button
                                                }
                                                actionInProgress = catBadge.code
                                                coroutineScope.launch {
                                                    val res = com.example.BadgeManager.awardBadge(context, targetUserId.trim(), catBadge.code)
                                                    if (res.isSuccess) {
                                                        Toast.makeText(context, "Awarded ${catBadge.name}!", Toast.LENGTH_SHORT).show()
                                                        refreshUserBadges(targetUserId)
                                                        onBadgeModified()
                                                    } else {
                                                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Award failed", Toast.LENGTH_LONG).show()
                                                    }
                                                    actionInProgress = null
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isAlreadyAwarded) funkyColors.soft else funkyColors.mango,
                                                contentColor = if (isAlreadyAwarded) funkyColors.mute else funkyColors.onMango
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            enabled = actionInProgress == null && !isAlreadyAwarded
                                        ) {
                                            Text(
                                                text = if (isAlreadyAwarded) "Awarded" else "Award",
                                                fontSize = 11.sp,
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
}

@Composable
fun AdminFeedbackInboxView() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val funkyColors = LocalFunkyColors.current

    var kindFilter by remember { mutableStateOf<String?>(null) } // null = All, "bug", "feature"
    var statusFilter by remember { mutableStateOf<String?>(null) } // null = All, "open", "planned", ...
    var items by remember { mutableStateOf<List<FeedbackPostDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFeedbackId by remember { mutableStateOf<Long?>(null) }

    fun loadFeedback() {
        scope.launch {
            isLoading = true
            FeedbackRepository.adminListFeedback(
                context = context,
                kind = kindFilter,
                status = statusFilter,
                limit = 30,
                offset = 0
            ).onSuccess { list ->
                items = list
            }.onFailure { err ->
                Toast.makeText(context, err.message ?: "Failed to load feedback", Toast.LENGTH_SHORT).show()
            }
            isLoading = false
        }
    }

    LaunchedEffect(kindFilter, statusFilter) {
        loadFeedback()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Kind filter row (All / Bugs / Features)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(null to "All", "bug" to "Bugs", "feature" to "Features").forEach { (key, label) ->
                val active = kindFilter == key
                Surface(
                    onClick = { kindFilter = key },
                    shape = RoundedCornerShape(99.dp),
                    color = if (active) funkyColors.mango else funkyColors.surface,
                    border = BorderStroke(1.dp, if (active) funkyColors.mango else funkyColors.line)
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) funkyColors.onMango else funkyColors.ink,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { loadFeedback() }) {
                SoftRefreshIcon(size = 18.dp, color = funkyColors.ink)
            }
        }

        // Status filter row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val statuses = listOf(
                null to "All Statuses",
                "open" to "Open",
                "planned" to "Planned",
                "in_progress" to "In Progress",
                "done" to "Done",
                "declined" to "Declined",
                "duplicate" to "Duplicate"
            )
            items(statuses) { (key, label) ->
                val active = statusFilter == key
                Surface(
                    onClick = { statusFilter = key },
                    shape = RoundedCornerShape(10.dp),
                    color = if (active) funkyColors.soft else funkyColors.surface,
                    border = BorderStroke(1.dp, if (active) funkyColors.mango else funkyColors.line)
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) funkyColors.ink else funkyColors.mute,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading && items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = funkyColors.mango)
            }
        } else if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("No feedback items match this filter.", fontSize = 13.sp, color = funkyColors.mute)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    Card(
                        onClick = { selectedFeedbackId = item.id },
                        colors = CardDefaults.cardColors(containerColor = funkyColors.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, funkyColors.line),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FeedbackKindChip(kind = item.kind)
                                FeedbackStatusChip(status = item.status)
                                Spacer(modifier = Modifier.weight(1f))
                                if (item.kind == "feature") {
                                    Text(
                                        text = "${item.votes_count} votes",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = funkyColors.mango
                                    )
                                }
                                Text(
                                    text = SupportChatManager.formatMessageTime(item.created_at),
                                    fontSize = 11.sp,
                                    color = funkyColors.mute
                                )
                            }

                            Text(
                                text = item.title,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = funkyColors.ink,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (item.body.isNotBlank()) {
                                Text(
                                    text = item.body,
                                    fontSize = 12.sp,
                                    color = funkyColors.mute,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            val authorText = item.author?.name?.takeIf { it.isNotBlank() }
                                ?: item.author?.username?.takeIf { it.isNotBlank() }
                                ?: item.author?.id?.take(8)
                                ?: "User"
                            val diagText = listOfNotNull(
                                item.app_version?.takeIf { it.isNotBlank() },
                                item.device_info?.takeIf { it.isNotBlank() }
                            ).joinToString(" · ")

                            Text(
                                text = if (diagText.isNotBlank()) "By $authorText · $diagText" else "By $authorText · ${item.replies_count} replies",
                                fontSize = 11.sp,
                                color = funkyColors.mute,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    selectedFeedbackId?.let { fId ->
        AdminFeedbackDetailDialog(
            feedbackId = fId,
            onDismiss = {
                selectedFeedbackId = null
                loadFeedback()
            }
        )
    }
}

@Composable
private fun AdminFeedbackDetailDialog(
    feedbackId: Long,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val funkyColors = LocalFunkyColors.current

    var post by remember { mutableStateOf<FeedbackPostDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedStatus by remember { mutableStateOf("open") }
    var statusNote by remember { mutableStateOf("") }
    var isUpdatingStatus by remember { mutableStateOf(false) }
    var replyBody by remember { mutableStateOf("") }
    var isSendingReply by remember { mutableStateOf(false) }

    fun loadDetail() {
        scope.launch {
            isLoading = true
            FeedbackRepository.getFeedback(context, feedbackId)
                .onSuccess { dto ->
                    post = dto
                    selectedStatus = dto.status
                }
                .onFailure { err ->
                    Toast.makeText(context, err.message ?: "Could not load item", Toast.LENGTH_SHORT).show()
                }
            isLoading = false
        }
    }

    LaunchedEffect(feedbackId) {
        loadDetail()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            shape = RoundedCornerShape(24.dp),
            color = funkyColors.bg,
            border = BorderStroke(1.dp, funkyColors.line)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(funkyColors.surface)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Feedback #$feedbackId",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = funkyColors.ink
                    )
                    IconButton(onClick = onDismiss) {
                        SoftCloseIcon(size = 18.dp, color = funkyColors.ink)
                    }
                }
                HorizontalDivider(color = funkyColors.line)

                if (isLoading && post == null) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = funkyColors.mango)
                    }
                } else if (post != null) {
                    val item = post!!
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = funkyColors.surface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, funkyColors.line),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FeedbackKindChip(kind = item.kind)
                                        FeedbackStatusChip(status = item.status)
                                    }
                                    Text(item.title, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = funkyColors.ink)
                                    if (item.body.isNotBlank()) {
                                        Text(item.body, fontSize = 13.5.sp, color = funkyColors.ink, lineHeight = 19.sp)
                                    }
                                    val reporter = item.author?.name ?: item.author?.username ?: item.author?.id ?: "Unknown"
                                    Text("Reporter: $reporter", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = funkyColors.mute)
                                    if (!item.app_version.isNullOrBlank() || !item.device_info.isNullOrBlank()) {
                                        Text(
                                            text = "Device: ${item.app_version ?: "-"} · ${item.device_info ?: "-"}",
                                            fontSize = 11.sp,
                                            color = funkyColors.mute
                                        )
                                    }
                                }
                            }
                        }

                        // Status Update Card
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = funkyColors.surface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, funkyColors.line),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Set Status & Team Note", fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = funkyColors.ink)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        val statuses = listOf("open", "planned", "in_progress", "done", "declined", "duplicate")
                                        items(statuses) { st ->
                                            val active = selectedStatus == st
                                            Surface(
                                                onClick = { selectedStatus = st },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (active) funkyColors.mango else funkyColors.soft,
                                                border = BorderStroke(1.dp, if (active) funkyColors.mango else funkyColors.line)
                                            ) {
                                                Text(
                                                    text = st.replace('_', ' '),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (active) funkyColors.onMango else funkyColors.ink,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                    OutlinedTextField(
                                        value = statusNote,
                                        onValueChange = { if (it.length <= 500) statusNote = it },
                                        placeholder = { Text("Optional team note (posted as reply to user)...", fontSize = 12.5.sp) },
                                        maxLines = 3,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Button(
                                        onClick = {
                                            isUpdatingStatus = true
                                            scope.launch {
                                                FeedbackRepository.adminSetFeedbackStatus(
                                                    context = context,
                                                    id = feedbackId,
                                                    status = selectedStatus,
                                                    note = statusNote.trim().takeIf { it.isNotEmpty() }
                                                ).onSuccess {
                                                    statusNote = ""
                                                    Toast.makeText(context, "Status updated to $selectedStatus", Toast.LENGTH_SHORT).show()
                                                    loadDetail()
                                                }.onFailure { err ->
                                                    Toast.makeText(context, err.message ?: "Update failed", Toast.LENGTH_SHORT).show()
                                                }
                                                isUpdatingStatus = false
                                            }
                                        },
                                        enabled = !isUpdatingStatus,
                                        colors = ButtonDefaults.buttonColors(containerColor = funkyColors.mango, contentColor = funkyColors.onMango),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (isUpdatingStatus) "Updating..." else "Update Status",
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Text("Thread (${item.replies.size})", fontSize = 13.5.sp, fontWeight = FontWeight.ExtraBold, color = funkyColors.ink)
                        }

                        items(item.replies, key = { it.id }) { rep: FeedbackReplyDto ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (rep.is_staff) funkyColors.soft else funkyColors.surface,
                                border = BorderStroke(1.dp, if (rep.is_staff) funkyColors.mango.copy(alpha = 0.5f) else funkyColors.line),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(
                                            text = if (rep.is_staff) "Funkytalk Team" else (rep.author?.name ?: rep.author?.username ?: "User"),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (rep.is_staff) funkyColors.mango else funkyColors.ink
                                        )
                                        Text(SupportChatManager.formatMessageTime(rep.created_at), fontSize = 10.5.sp, color = funkyColors.mute)
                                    }
                                    Text(rep.body, fontSize = 13.sp, color = funkyColors.ink)
                                }
                            }
                        }
                    }

                    Surface(color = funkyColors.surface, border = BorderStroke(1.dp, funkyColors.line)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = replyBody,
                                onValueChange = { if (it.length <= 1000) replyBody = it },
                                placeholder = { Text("Reply as FunkyTalk Team...", fontSize = 13.sp) },
                                maxLines = 3,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            Button(
                                onClick = {
                                    val txt = replyBody.trim()
                                    if (txt.isEmpty()) return@Button
                                    isSendingReply = true
                                    scope.launch {
                                        FeedbackRepository.addFeedbackReply(context, feedbackId, txt)
                                            .onSuccess {
                                                replyBody = ""
                                                loadDetail()
                                            }
                                            .onFailure { err ->
                                                Toast.makeText(context, err.message ?: "Reply failed", Toast.LENGTH_SHORT).show()
                                            }
                                        isSendingReply = false
                                    }
                                },
                                enabled = replyBody.isNotBlank() && !isSendingReply,
                                colors = ButtonDefaults.buttonColors(containerColor = funkyColors.mango, contentColor = funkyColors.onMango),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(if (isSendingReply) "..." else "Send", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
