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
import com.example.SupabaseClient
import com.example.SupabaseMoment
import com.example.SupabaseSessionManager
import com.example.UserProfile
import com.example.ui.components.*
import com.example.ui.screens.home.models.DarkText
import com.example.ui.screens.home.models.GraySubtitle
import com.example.ui.theme.GoldPrimary
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

                        mp.setOnErrorListener { _, what, extra ->
                            isBuffering = false
                            isPlaying = false
                            playbackError = "Playback error ($what, $extra)"
                            try {
                                localAudioFile.delete()
                            } catch (_: Exception) {}
                            true
                        }

                        mp.prepareAsync()
                    } catch (e: Exception) {
                        isBuffering = false
                        isPlaying = false
                        playbackError = e.localizedMessage ?: "Player failed"
                        try {
                            localAudioFile.delete()
                        } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isBuffering = false
                    isPlaying = false
                    playbackError = e.localizedMessage ?: "Download failed"
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
                    errorMessage = "Network error: ${e.localizedMessage ?: "Unable to fetch data"}"
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header Bar
                    Surface(
                        color = Color.White,
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
                                        .background(Color(0xFFFEF3C7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SoftShieldIcon(size = 22.dp, color = Color(0xFFD97706))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Moderation Center",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp,
                                            color = DarkText
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFFD1FAE5))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "ADMIN",
                                                color = Color(0xFF065F46),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Review incident reports & enforce safety",
                                        color = Color(0xFF6B7280),
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
                                        .background(Color(0xFFF3F4F6))
                                ) {
                                    SoftRefreshIcon(size = 18.dp, color = Color(0xFF374151))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF3F4F6))
                                        .testTag("close_moderation_dialog")
                                ) {
                                    SoftCloseIcon(size = 16.dp, color = Color(0xFF374151))
                                }
                            }
                        }
                    }

                    // Admin Navigation Tabs
                    PrimaryTabRow(
                        selectedTabIndex = selectedAdminTab,
                        containerColor = Color.White,
                        contentColor = Color(0xFFD97706),
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
                                    SoftBlockIcon(size = 16.dp, color = Color(0xFFDC2626))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Active Penalties (${activePenaltiesList.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        )
                        Tab(
                            selected = selectedAdminTab == 2,
                            onClick = { selectedAdminTab == 2 },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SoftHeadsetIcon(size = 16.dp, color = Color(0xFF4F46E5))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Support Inbox", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    if (adminUnreadSupportCount > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(Color(0xFFDC2626))
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
                                    SoftStarIcon(size = 16.dp, color = Color(0xFFD97706))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Badges", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE5E7EB), thickness = 1.dp)

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
                                    color = DarkText
                                )
                                Text(
                                    text = "Manage and revoke active warnings, suspensions, or mutes across FunkyTalk.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6B7280)
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
                                        CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(32.dp))
                                    }
                                }
                            } else if (activePenaltiesList.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 20.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
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
                                                    .background(Color(0xFFF3F4F6)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                SoftShieldIcon(size = 28.dp, color = Color(0xFF10B981))
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "No Active Restrictions",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = DarkText
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "There are currently no users with active warning, suspend, or mute actions.",
                                                fontSize = 12.sp,
                                                color = GraySubtitle
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(activePenaltiesList, key = { it.user_id ?: UUID.randomUUID().toString() }) { penalty ->
                                    val actionType = penalty.action_type?.lowercase() ?: "action"
                                    val badgeColor = when (actionType) {
                                        "suspend" -> Color(0xFFDC2626)
                                        "warning" -> Color(0xFFD97706)
                                        "mute" -> Color(0xFF7C3AED)
                                        else -> Color(0xFF4B5563)
                                    }
                                    val badgeBg = when (actionType) {
                                        "suspend" -> Color(0xFFFEE2E2)
                                        "warning" -> Color(0xFFFEF3C7)
                                        "mute" -> Color(0xFFEDE9FE)
                                        else -> Color(0xFFF3F4F6)
                                    }

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
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
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                ) {
                                                    SoftSwapIcon(size = 14.dp, color = Color.White)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Revoke Action", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

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
                                                    color = Color(0xFF2563EB),
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                SoftCopyIcon(size = 12.dp, color = Color(0xFF2563EB))
                                            }

                                            if (!penalty.reason.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Reason: ${penalty.reason}",
                                                    fontSize = 12.sp,
                                                    color = DarkText
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = if (penalty.expires_at != null) "Expires: ${penalty.expires_at}" else "Expires: Indefinite / Manual Revoke",
                                                fontSize = 11.sp,
                                                color = Color(0xFF6B7280)
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
    val scope = rememberCoroutineScope()
    var reasonInput by remember { mutableStateOf("") }
    var isRevoking by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { if (!isRevoking) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                            .background(Color(0xFFECFDF5)),
                        contentAlignment = Alignment.Center
                    ) {
                        SoftSwapIcon(size = 20.dp, color = Color(0xFF10B981))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Revoke Moderation Action", fontWeight = FontWeight.Black, fontSize = 16.sp, color = DarkText)
                        Text("Lift warning, suspension, or mute", fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Target User UUID:", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6B7280))
                        Text(
                            text = targetUserId,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2563EB),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Revocation Reason:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkText)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = { reasonInput = it },
                    placeholder = { Text("Reason for revoking (e.g., appeal granted, resolved)...", fontSize = 12.sp, color = Color(0xFF9CA3AF)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color(0xFFE5E7EB)
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
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Cancel", color = Color(0xFF6B7280), fontWeight = FontWeight.Bold)
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
                                    Toast.makeText(context, "✓ $msg", Toast.LENGTH_LONG).show()
                                    ModerationManager.fetchMyModerationStatus(context)
                                    onRevoked()
                                }.onFailure { e ->
                                    Toast.makeText(context, "Revoke Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isRevoking,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                            .background(Color(0xFFFEF2F2)),
                        contentAlignment = Alignment.Center
                    ) {
                        SoftGavelIcon(size = 20.dp, color = Color(0xFFEF4444))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Apply Moderation Action", fontWeight = FontWeight.Black, fontSize = 17.sp, color = DarkText)
                        val contextLabel = when (report.context_type?.lowercase()) {
                            "profile" -> "Profile Incident"
                            "dm" -> "Direct Message Incident"
                            "moment" -> "Moment Post Incident"
                            else -> "Room #${report.room_id?.take(8) ?: "N/A"}"
                        }
                        Text(contextLabel, fontSize = 12.sp, color = Color(0xFF6B7280))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Target Offender Selection
                Text("Target Offender:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkText)
                Spacer(modifier = Modifier.height(6.dp))

                if (!prefilledTarget.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEFF6FF),
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
                                    .background(Color(0xFFDBEAFE)),
                                contentAlignment = Alignment.Center
                            ) {
                                SoftPersonIcon(size = 18.dp, color = Color(0xFF2563EB))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Reported Offender", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
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
                                Text(prefilledTarget, fontSize = 12.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else if (validTargets.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SoftWarningIcon(size = 18.dp, color = Color(0xFFD97706))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "No reported user UUID attached. Please enter the target UUID below.",
                                fontSize = 12.sp,
                                color = Color(0xFF92400E)
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
                                color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF9FAFB),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF3B82F6) else Color(0xFFE5E7EB)),
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
                                                    color = DarkText
                                                )
                                                if (item.user_id == report.reporter_id) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "(Reporter)",
                                                        color = Color(0xFFDC2626),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Role: ${item.role} • UUID: ${item.user_id.take(8)}...",
                                                fontSize = 11.sp,
                                                color = Color(0xFF6B7280)
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
                    textStyle = TextStyle(fontSize = 12.sp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (effectiveTargetUserId.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Target ID: $effectiveTargetUserId",
                        fontSize = 11.sp,
                        color = Color(0xFF2563EB),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Type Selector
                Text("Enforcement Type:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkText)
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
                            color = if (isSelected) Color(0xFF1E293B) else Color(0xFFF3F4F6),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else Color(0xFF374151),
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
                                Text("Suspension Duration:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = DarkText)
                                Text("$durationHours hours", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
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
                                        color = if (isSelected) Color(0xFFDC2626) else Color(0xFFF3F4F6),
                                        modifier = Modifier.weight(1f).height(30.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${hours}h",
                                                color = if (isSelected) Color.White else Color(0xFF374151),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Blocks voice room access and messaging (1 to 90 hours).", fontSize = 11.sp, color = Color(0xFF6B7280))
                        }
                    }
                    "warning" -> {
                        Text("⚠️ Fixed 1 hour penalty: Messaging disabled, user cannot send chat messages or replies.", fontSize = 11.sp, color = Color(0xFFB45309))
                    }
                    "mute" -> {
                        Text("🔇 Indefinite Mute: User outgoing messaging is blocked until revoked.", fontSize = 11.sp, color = Color(0xFF4B5563))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Reason input
                OutlinedTextField(
                    value = reasonInput,
                    onValueChange = { reasonInput = it },
                    placeholder = { Text("Reason for moderation...", fontSize = 12.sp, color = Color(0xFF9CA3AF)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color(0xFFE5E7EB)
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
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Cancel", color = Color(0xFF6B7280), fontWeight = FontWeight.Bold)
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
                                    Toast.makeText(context, "✓ $msg", Toast.LENGTH_LONG).show()
                                    onActionApplied()
                                }.onFailure { e ->
                                    Toast.makeText(context, "Enforcement Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isSubmitting && effectiveTargetUserId.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
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
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
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
                color = DarkText
            )
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF6B7280),
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
    val isSolved = (report.status ?: "").lowercase() in listOf("solved", "resolved", "closed")
    val isUrgent = (report.category ?: "").contains("Abuse", ignoreCase = true) || (report.category ?: "").contains("Harassment", ignoreCase = true)
    val contextType = (report.context_type ?: "room").lowercase().trim()

    var isSnapshotsExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Color(0x12000000)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (isUrgent && !isSolved) Color(0xFFFECACA) else Color(0xFFE5E7EB))
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
                        isUrgent -> Color(0xFFFEF2F2) to Color(0xFFDC2626)
                        (report.category ?: "").contains("Spam", ignoreCase = true) -> Color(0xFFF3E8FF) to Color(0xFF7E22CE)
                        else -> Color(0xFFFFFBEB) to Color(0xFFD97706)
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
                        "profile" -> Triple(Color(0xFFEFF6FF), Color(0xFF1D4ED8), "👤 Profile")
                        "dm" -> Triple(Color(0xFFFAF5FF), Color(0xFF7E22CE), "💬 E2EE DM")
                        "moment" -> Triple(Color(0xFFECFDF5), Color(0xFF047857), "📸 Moment")
                        else -> Triple(Color(0xFFFFF7ED), Color(0xFFC2410C), "🎙️ Voice Room")
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ctxBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(ctxLabel, color = ctxText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (isUrgent && !isSolved) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFDC2626))
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
                        .background(if (isSolved) Color(0xFFECFDF5) else Color(0xFFFFFBEB))
                        .border(
                            1.dp,
                            if (isSolved) Color(0xFFA7F3D0) else Color(0xFFFDE68A),
                            RoundedCornerShape(100.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isSolved) "✓ Resolved" else "● Pending",
                        color = if (isSolved) Color(0xFF047857) else Color(0xFFB45309),
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
                color = DarkText
            )

            if (!report.additional_details.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = report.additional_details,
                    fontSize = 13.sp,
                    color = Color(0xFF4B5563),
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
                                .background(Color(0xFFF3F4F6))
                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
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
                                            color = GoldPrimary
                                        )
                                    }
                                },
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFFFEF2F2)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            SoftImageIcon(size = 18.dp, color = Color(0xFFEF4444))
                                            Text("Evidence", fontSize = 10.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
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
                                .background(if (isThisPlaying) Color(0xFFFFFBEB) else Color(0xFFF9FAFB))
                                .border(
                                    1.dp,
                                    if (isThisPlaying) Color(0xFFFCD34D) else Color(0xFFE5E7EB),
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
                                            .background(if (isThisPlaying) Color(0xFFD97706) else Color(0xFF1E293B)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isThisBuffering) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else if (isThisPlaying) {
                                            SoftPauseIcon(size = 16.dp, color = Color.White)
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
                                            color = DarkText,
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
                                        color = Color(0xFFD97706),
                                        trackColor = Color(0xFFE5E7EB)
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
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                                        color = Color(0xFF475569)
                                    )
                                }
                                SoftChevronDownIcon(
                                    size = 14.dp,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.rotate(if (isSnapshotsExpanded) 180f else 0f)
                                )
                            }

                            if (isSnapshotsExpanded) {
                                Spacer(modifier = Modifier.height(8.dp))

                                // Speaker log list
                                if (hasSpeakers) {
                                    Text("Speakers Captured:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkText)
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
                                                color = Color(0xFF334155)
                                            )
                                            Text(
                                                text = "ID: ${spk.user_id.take(8)}...",
                                                fontSize = 10.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                }

                                // Message snapshot
                                if (hasMessages) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Chat Messages Snapshot:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkText)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    report.message_snapshot!!.takeLast(5).forEach { msg ->
                                        Text(
                                            text = "${msg.sender}: ${msg.text}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF475569),
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
                colors = ButtonDefaults.buttonColors(containerColor = if (isSolved) Color(0xFF475569) else Color(0xFFDC2626)),
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
                    .background(Color(0xFFEFF6FF))
                    .clickable { onCopyText(reportedUserId, "Target User ID") }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SoftPersonIcon(size = 13.dp, color = Color(0xFF2563EB))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Target: ${reportedUserId.take(8)}...", fontSize = 11.sp, color = Color(0xFF1E40AF), fontWeight = FontWeight.SemiBold)
                    }
                    SoftCopyIcon(size = 11.dp, color = Color(0xFF3B82F6))
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF3F4F6))
                    .clickable { onCopyText(reporterId, "Reporter ID") }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SoftPersonIcon(size = 13.dp, color = Color(0xFF6B7280))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reporter: ${reporterId.take(8)}...", fontSize = 11.sp, color = Color(0xFF374151), fontWeight = FontWeight.Medium)
                    }
                    SoftCopyIcon(size = 11.dp, color = Color(0xFF9CA3AF))
                }
            }
        }

        // Mini Profile Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isFetchingProfile) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = GoldPrimary)
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
                            color = DarkText
                        )
                        Text(
                            text = "@${p.username ?: "user"}",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B)
                        )
                        if (!p.bio.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = p.bio,
                                fontSize = 12.sp,
                                color = Color(0xFF334155),
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
                    color = Color(0xFF64748B),
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFAF5FF))
                    .clickable { onCopyText(reportedUserId, "Reported Sender ID") }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SoftPersonIcon(size = 13.dp, color = Color(0xFF7E22CE))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sender: ${reportedUserId.take(8)}...", fontSize = 11.sp, color = Color(0xFF6B21A8), fontWeight = FontWeight.SemiBold)
                    }
                    SoftCopyIcon(size = 11.dp, color = Color(0xFFA855F7))
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF3F4F6))
                    .clickable { onCopyText(reporterId, "Reporter ID") }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SoftPersonIcon(size = 13.dp, color = Color(0xFF6B7280))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reporter: ${reporterId.take(8)}...", fontSize = 11.sp, color = Color(0xFF374151), fontWeight = FontWeight.Medium)
                    }
                    SoftCopyIcon(size = 11.dp, color = Color(0xFF9CA3AF))
                }
            }
        }

        // E2EE Notice Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFF3E8FF)),
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
                    color = Color(0xFF6B21A8),
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
    var momentData by remember { mutableStateOf<SupabaseMoment?>(null) }
    var isFetchingMoment by remember { mutableStateOf(true) }

    LaunchedEffect(momentId) {
        if (momentId.isNotBlank()) {
            isFetchingMoment = true
            withContext(Dispatchers.IO) {
                try {
                    val auth = SupabaseSessionManager.getAuthenticatedBearerToken(context) ?: BuildConfig.SUPABASE_ANON_KEY
                    val res = SupabaseClient.api.getMomentById(
                        auth = auth,
                        idEq = "eq.$momentId"
                    )
                    if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                        withContext(Dispatchers.Main) {
                            momentData = res.body()!!.first()
                            isFetchingMoment = false
                        }
                    } else {
                        withContext(Dispatchers.Main) { isFetchingMoment = false }
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) { isFetchingMoment = false }
                }
            }
        } else {
            isFetchingMoment = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFECFDF5))
                    .clickable { onCopyText(momentId, "Moment Post ID") }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SoftImageIcon(size = 13.dp, color = Color(0xFF047857))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Post: ${momentId.take(8)}...", fontSize = 11.sp, color = Color(0xFF065F46), fontWeight = FontWeight.SemiBold)
                    }
                    SoftCopyIcon(size = 11.dp, color = Color(0xFF10B981))
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFF3F4F6))
                    .clickable { onCopyText(reporterId, "Reporter ID") }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SoftPersonIcon(size = 13.dp, color = Color(0xFF6B7280))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reporter: ${reporterId.take(8)}...", fontSize = 11.sp, color = Color(0xFF374151), fontWeight = FontWeight.Medium)
                    }
                    SoftCopyIcon(size = 11.dp, color = Color(0xFF9CA3AF))
                }
            }
        }

        // Moment Post Preview Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFF8FAFC),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isFetchingMoment) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = GoldPrimary)
                }
            } else if (momentData != null) {
                val m = momentData!!
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = m.content ?: "[No post text]",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkText
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Image thumbnail if attached to the moment
                    if (!m.image_urls.isNullOrEmpty()) {
                        val firstImg = m.image_urls.first()
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE2E8F0))
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
                            Text("${m.likes_count ?: 0} likes", fontSize = 11.sp, color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.width(10.dp))
                            SoftChatBubbleIcon(size = 14.dp, color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("${m.comments_count ?: 0} comments", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                        Text(
                            text = "Author: ${(m.user_id ?: reportedUserId).take(8)}...",
                            fontSize = 10.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            } else {
                Text(
                    text = "Reported Moment Post ID: $momentId",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF3F4F6))
                .clickable { onCopyText(roomId, "Room ID") }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SoftGlobeIcon(size = 13.dp, color = Color(0xFF6B7280))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Room: ${roomId.take(8)}...", fontSize = 11.sp, color = Color(0xFF374151), fontWeight = FontWeight.Medium)
                }
                SoftCopyIcon(size = 11.dp, color = Color(0xFF9CA3AF))
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF3F4F6))
                .clickable { onCopyText(reporterId, "Reporter ID") }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SoftPersonIcon(size = 13.dp, color = Color(0xFF6B7280))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reporter: ${reporterId.take(8)}...", fontSize = 11.sp, color = Color(0xFF374151), fontWeight = FontWeight.Medium)
                }
                SoftCopyIcon(size = 11.dp, color = Color(0xFF9CA3AF))
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(top = 2.dp)
    ) {
        Box(modifier = Modifier.width(3.dp).height(h1.dp).clip(RoundedCornerShape(2.dp)).background(if (isPlaying) Color(0xFFD97706) else Color(0xFF9CA3AF)))
        Box(modifier = Modifier.width(3.dp).height(h2.dp).clip(RoundedCornerShape(2.dp)).background(if (isPlaying) Color(0xFFD97706) else Color(0xFF9CA3AF)))
        Box(modifier = Modifier.width(3.dp).height(h3.dp).clip(RoundedCornerShape(2.dp)).background(if (isPlaying) Color(0xFFD97706) else Color(0xFF9CA3AF)))
        Box(modifier = Modifier.width(3.dp).height(h1.dp).clip(RoundedCornerShape(2.dp)).background(if (isPlaying) Color(0xFFD97706) else Color(0xFF9CA3AF)))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = timeText,
            fontSize = 10.sp,
            color = if (isPlaying) Color(0xFFD97706) else Color(0xFF6B7280),
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
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
                            .background(Color(0xFFF59E0B)),
                        contentAlignment = Alignment.Center
                    ) {
                        SoftStarIcon(size = 22.dp, color = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Badges & Honors Management",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "Manually award or revoke prestigious badges (e.g. Founder, VIP) to community members.",
                            fontSize = 12.sp,
                            color = Color(0xFFB45309),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // 2. Target User Input / Picker
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Target User UUID / ID",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF374151)
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD43B), contentColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            SoftSearchIcon(size = 14.dp, color = Color(0xFF0F172A))
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
                            color = Color(0xFF9CA3AF)
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(supabaseProfiles.take(8)) { profile ->
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .clickable {
                                            targetUserId = profile.uid
                                            refreshUserBadges(profile.uid)
                                        },
                                    color = if (targetUserId == profile.uid) Color(0xFFFEF3C7) else Color(0xFFF3F4F6),
                                    shape = RoundedCornerShape(100.dp),
                                    border = BorderStroke(1.dp, if (targetUserId == profile.uid) Color(0xFFF59E0B) else Color(0xFFE5E7EB))
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
                                            color = if (targetUserId == profile.uid) Color(0xFF92400E) else Color(0xFF374151)
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
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
                            color = Color(0xFF1F2937)
                        )
                        if (isLoadingUserBadges) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color(0xFFD97706))
                        }
                    }

                    if (targetUserId.isBlank()) {
                        Text(
                            text = "Enter a user ID above to view and manage their badges.",
                            fontSize = 12.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    } else if (userBadges.isEmpty()) {
                        Text(
                            text = "This user currently has no assigned badges.",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            userBadges.forEach { badge ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF9FAFB),
                                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
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
                                                    color = Color(0xFF111827)
                                                )
                                                Text(
                                                    text = "code: ${badge.code}",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF9CA3AF)
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
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2), contentColor = Color(0xFFDC2626)),
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Available Badges in Catalog",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937)
                    )

                    if (isLoadingCatalog) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFFD97706))
                    } else if (catalogBadges.isEmpty()) {
                        Text("No badges found in catalog.", fontSize = 12.sp, color = Color(0xFF9CA3AF))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            catalogBadges.forEach { catBadge ->
                                val isAlreadyAwarded = userBadges.any { it.code.equals(catBadge.code, ignoreCase = true) }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isAlreadyAwarded) Color(0xFFF3F4F6) else Color(0xFFFFFBEB).copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, if (isAlreadyAwarded) Color(0xFFE5E7EB) else Color(0xFFFDE68A)),
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
                                                    color = Color(0xFF1F2937)
                                                )
                                                Text(
                                                    text = catBadge.description ?: "Special community badge",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF6B7280),
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
                                                containerColor = if (isAlreadyAwarded) Color(0xFFE5E7EB) else Color(0xFFFFD43B),
                                                contentColor = Color(0xFF0F172A)
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
