package com.example.ui.screens.home.dialogs

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.platform.testTag
import com.example.AdminSendSupportMessageRequest
import com.example.AdminSupportManager
import com.example.BuildConfig
import com.example.CreateSignedUrlRequest
import com.example.ModerationManager
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.SupabaseSupportMessage
import com.example.ui.components.*
import com.example.ui.screens.home.models.DarkText
import com.example.ui.screens.home.models.GraySubtitle
import com.example.ui.theme.GoldPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.Instant
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportDetailDialog(
    reportId: String,
    onDismiss: () -> Unit,
    onReportUpdated: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var detailJson by remember { mutableStateOf<JSONObject?>(null) }

    var isSuperAdmin by remember { mutableStateOf(false) }
    var currentAdminUid by remember { mutableStateOf<String?>(null) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    // Dialog action states
    var showActionDialog by remember { mutableStateOf(false) }
    var selectedActionType by remember { mutableStateOf("suspend") } // "suspend", "warning", "mute"
    var suspendHours by remember { mutableIntStateOf(24) }
    var actionReasonInput by remember { mutableStateOf("") }
    var isSubmittingAction by remember { mutableStateOf(false) }

    var showDismissDialog by remember { mutableStateOf(false) }
    var dismissReasonInput by remember { mutableStateOf("") }
    var isDismissing by remember { mutableStateOf(false) }

    var showMessageUserDialog by remember { mutableStateOf(false) }
    var messageUserInput by remember { mutableStateOf("") }
    var messageOpenHours by remember { mutableIntStateOf(2) } // default 2 hours
    var isSendingUserMessage by remember { mutableStateOf(false) }
    var chatOpenUntilTime by remember { mutableStateOf<String?>(null) }
    var isClosingSupportChat by remember { mutableStateOf(false) }

    var showReopenDialog by remember { mutableStateOf(false) }
    var reopenBriefInput by remember { mutableStateOf("") }
    var isSubmittingReopen by remember { mutableStateOf(false) }

    var showChallengeDialog by remember { mutableStateOf(false) }
    var activeChallengeReopenId by remember { mutableStateOf<String?>(null) }
    var challengeBriefInput by remember { mutableStateOf("") }
    var isSubmittingChallenge by remember { mutableStateOf(false) }

    var showDisputeDecisionDialog by remember { mutableStateOf(false) }
    var activeDisputeReopenId by remember { mutableStateOf<String?>(null) }
    var disputeNoteInput by remember { mutableStateOf("") }
    var isSubmittingDecision by remember { mutableStateOf(false) }

    fun loadDetail() {
        isLoading = true
        errorMessage = null
        scope.launch(Dispatchers.IO) {
            currentAdminUid = SupabaseSessionManager.getUserId(context)
            isSuperAdmin = ModerationManager.checkIsSuperAdmin(context)

            val res = ModerationManager.fetchReportDetail(context, reportId)
            withContext(Dispatchers.Main) {
                isLoading = false
                if (res.isSuccess) {
                    detailJson = res.getOrNull()
                } else {
                    errorMessage = res.exceptionOrNull()?.message ?: "Failed to load report detail."
                }
            }
        }
    }

    LaunchedEffect(reportId) {
        loadDetail()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f)
                .clip(RoundedCornerShape(24.dp)),
            color = Color(0xFFF9FAFB),
            shadowElevation = 12.dp
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
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFEF3C7)),
                                contentAlignment = Alignment.Center
                            ) {
                                SoftShieldIcon(size = 20.dp, color = Color(0xFFD97706))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Report #${reportId.take(8)}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = DarkText
                                )
                                Text(
                                    text = "Moderation Investigation & Workflow",
                                    fontSize = 11.5.sp,
                                    color = GraySubtitle
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { loadDetail() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF3F4F6))
                            ) {
                                SoftRefreshIcon(size = 16.dp, color = Color(0xFF374151))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF3F4F6))
                            ) {
                                SoftCloseIcon(size = 14.dp, color = Color(0xFF374151))
                            }
                        }
                    }
                }

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GoldPrimary)
                    }
                } else if (errorMessage != null || detailJson == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                            SoftWarningIcon(size = 32.dp, color = Color(0xFFDC2626))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = errorMessage ?: "Unable to fetch report details",
                                color = Color(0xFF991B1B),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { loadDetail() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                } else {
                    val root = detailJson!!
                    val reportObj = root.optJSONObject("report") ?: JSONObject()
                    val claimObj = root.optJSONObject("claim") ?: JSONObject()
                    val reporterObj = root.optJSONObject("reporter") ?: JSONObject()
                    val reportedObj = root.optJSONObject("reported") ?: JSONObject()
                    val reopensArr = root.optJSONArray("reopens") ?: JSONArray()
                    val eventsArr = root.optJSONArray("events") ?: JSONArray()

                    val reportStatus = reportObj.optString("status", "pending")
                    val outcome = reportObj.optString("outcome").takeIf { it.isNotBlank() && it != "null" }
                    val resolvedAt = reportObj.optString("resolved_at").takeIf { it.isNotBlank() && it != "null" }
                    val isResolved = reportStatus == "resolved" && outcome != null

                    val isClaimActive = claimObj.optBoolean("active", false)
                    val isMine = claimObj.optBoolean("is_mine", false)
                    val claimedByName = claimObj.optString("claimed_by_name").takeIf { it.isNotBlank() && it != "null" } ?: "Staff Admin"
                    val claimExpiresAt = claimObj.optString("expires_at").takeIf { it.isNotBlank() && it != "null" }

                    val reportedUserId = reportedObj.optString("user_id").takeIf { it.isNotBlank() && it != "null" }
                        ?: reportObj.optString("reported_user_id")
                    val reportedName = reportedObj.optString("name").takeIf { it.isNotBlank() && it != "null" } ?: "Reported User"
                    val reportedScore = reportedObj.optInt("score", 100)

                    val reporterName = reporterObj.optString("name").takeIf { it.isNotBlank() && it != "null" } ?: "Anonymous Reporter"

                    val contextType = reportObj.optString("context_type", "report").lowercase()
                    val evidenceStatus = reportObj.optString("evidence_status", "none")

                    // Extract proof containers
                    val evidenceObj = parseJsonObject(reportObj.opt("evidence"))
                    val messageSnapshotArr = parseJsonArray(reportObj.opt("message_snapshot"))
                    val speakerLogObj = parseJsonObject(reportObj.opt("speaker_log"))
                    val deletionLogArr = parseJsonArray(reportObj.opt("deletion_log"))

                    val hasProof = evidenceObj != null ||
                            (messageSnapshotArr != null && messageSnapshotArr.length() > 0) ||
                            speakerLogObj != null ||
                            (deletionLogArr != null && deletionLogArr.length() > 0)

                    // Live countdown ticker for claim expiration
                    var remainingTimeFormatted by remember(claimExpiresAt) {
                        mutableStateOf(formatCountdown(claimExpiresAt))
                    }

                    LaunchedEffect(claimExpiresAt, isClaimActive) {
                        if (isClaimActive && !claimExpiresAt.isNullOrBlank()) {
                            while (isActive) {
                                remainingTimeFormatted = formatCountdown(claimExpiresAt)
                                delay(1000L)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Claim Status & Countdown Banner
                        if (isClaimActive) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isMine) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isMine) Color(0xFFFCD34D) else Color(0xFFCBD5E1)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(if (isMine) Color(0xFFF59E0B) else Color(0xFF64748B)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            SoftShieldIcon(size = 18.dp, color = Color.White)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isMine) "In your charge (Claimed)" else "In charge: $claimedByName",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = if (isMine) Color(0xFF92400E) else Color(0xFF334155)
                                            )
                                            Text(
                                                text = if (isMine) "Time left: $remainingTimeFormatted" else "Time remaining: $remainingTimeFormatted (Read-Only)",
                                                fontSize = 12.sp,
                                                color = if (isMine) Color(0xFFB45309) else Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    if (isMine) {
                                        TextButton(
                                            onClick = {
                                                scope.launch(Dispatchers.IO) {
                                                    val res = ModerationManager.releaseReport(context, reportId)
                                                    withContext(Dispatchers.Main) {
                                                        if (res.isSuccess) {
                                                            Toast.makeText(context, "Report released back to queue", Toast.LENGTH_SHORT).show()
                                                            loadDetail()
                                                            onReportUpdated()
                                                        } else {
                                                            Toast.makeText(context, res.exceptionOrNull()?.message ?: "Failed to release", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            }
                                        ) {
                                            Text("Release", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        } else if (reportStatus == "pending") {
                            // Free / Unclaimed banner with "Take charge" button
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Report is Free to Review",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color(0xFF1E3A8A)
                                        )
                                        Text(
                                            text = "Take charge to reserve this report for 48 hours and begin moderation.",
                                            fontSize = 12.sp,
                                            color = Color(0xFF3B82F6),
                                            lineHeight = 16.sp
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            scope.launch(Dispatchers.IO) {
                                                val res = ModerationManager.takeCharge(context, reportId)
                                                withContext(Dispatchers.Main) {
                                                    if (res.isSuccess) {
                                                        Toast.makeText(context, "You are now in charge of this report for 48 hours", Toast.LENGTH_LONG).show()
                                                        loadDetail()
                                                        onReportUpdated()
                                                    } else {
                                                        Toast.makeText(context, res.exceptionOrNull()?.message ?: "Failed to take charge", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Take Charge", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        } else if (isResolved) {
                            // Resolved banner
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        SoftCheckIcon(size = 20.dp, color = Color(0xFF16A34A))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Report Resolved (${outcome.uppercase()})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = Color(0xFF166534)
                                            )
                                            Text(
                                                text = "Outcome enforced by staff. Resolved ${resolvedAt?.take(10) ?: ""}",
                                                fontSize = 11.5.sp,
                                                color = Color(0xFF15803D)
                                            )
                                        }
                                    }

                                    val canReopen = remember(resolvedAt) {
                                        try {
                                            if (resolvedAt != null) {
                                                val instant = Instant.parse(resolvedAt)
                                                val days = Duration.between(instant, Instant.now()).toDays()
                                                days <= 30
                                            } else true
                                        } catch (_: Exception) { true }
                                    }

                                    if (canReopen) {
                                        Button(
                                            onClick = { showReopenDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("Reopen Report", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Incident Summary Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Incident Details", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkText)
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Context Badge
                                        Surface(
                                            color = Color(0xFFF1F5F9),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = contextType.uppercase(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF475569),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        // Evidence Status Badge (Requirement 1)
                                        EvidenceStatusBadge(status = evidenceStatus)
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFF1F5F9))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Category", fontSize = 11.sp, color = GraySubtitle)
                                        Text(reportObj.optString("category", "General"), fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = DarkText)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Reporter", fontSize = 11.sp, color = GraySubtitle)
                                        Text(reporterName, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, color = DarkText)
                                    }
                                }

                                Column {
                                    Text("Reason / Statement", fontSize = 11.sp, color = GraySubtitle)
                                    Text(
                                        text = reportObj.optString("reason", "No details provided."),
                                        fontSize = 13.sp,
                                        color = DarkText,
                                        lineHeight = 18.sp
                                    )
                                }

                                // Screenshot evidence if provided directly
                                val screenshotUrl = reportObj.optString("screenshot_url").takeIf { it.isNotBlank() && it != "null" }
                                if (screenshotUrl != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Attached Evidence Screenshot", fontSize = 11.sp, color = GraySubtitle)
                                    AsyncImage(
                                        model = screenshotUrl,
                                        contentDescription = "Evidence screenshot",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                            .clickable { previewImageUrl = screenshotUrl },
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }

                        // Reporter Proof Screenshots Section (Requirement 4)
                        ReporterProofScreenshotsSection(
                            mediaPathsArr = reportObj.optJSONArray("media_paths"),
                            onImageClick = { url -> previewImageUrl = url }
                        )

                        // 3. Reported User Card (Safety Score & Moderation History)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Reported User Profile", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkText)

                                    val scoreBg = when {
                                        reportedScore >= 80 -> Color(0xFFDCFCE7)
                                        reportedScore >= 50 -> Color(0xFFFEF3C7)
                                        else -> Color(0xFFFEE2E2)
                                    }
                                    val scoreText = when {
                                        reportedScore >= 80 -> Color(0xFF15803D)
                                        reportedScore >= 50 -> Color(0xFFB45309)
                                        else -> Color(0xFFB91C1C)
                                    }
                                    Surface(color = scoreBg, shape = RoundedCornerShape(10.dp)) {
                                        Text(
                                            text = "Safety Score: $reportedScore/100",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = scoreText,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFF1F5F9))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Name", fontSize = 11.sp, color = GraySubtitle)
                                        Text(reportedName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkText)
                                    }
                                    Column {
                                        Text("User ID", fontSize = 11.sp, color = GraySubtitle)
                                        Text(
                                            text = reportedUserId?.take(10) ?: "N/A",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                val otherReports = reportedObj.optJSONArray("other_reports") ?: JSONArray()
                                if (otherReports.length() > 0) {
                                    Text(
                                        text = "${otherReports.length()} other report(s) on record for this user",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }
                        }

                        // 4. PROOF AND EVIDENCE SECTIONS (Requirements 2, 3, 4, 5, 6, 8)
                        Text("Proof & Attached Evidence", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkText)

                        if (!hasProof) {
                            // Requirement 6: Empty proof card when no evidence exists
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        SoftShieldIcon(size = 32.dp, color = Color(0xFF94A3B8))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No proof was attached to this report",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.5.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Render specific proof components ONLY when they exist

                            // Moment Evidence (Context: "moment")
                            if (evidenceObj != null && contextType == "moment") {
                                MomentEvidenceCard(
                                    evidenceObj = evidenceObj,
                                    onImageClick = { url -> previewImageUrl = url }
                                )
                            }

                            // Room Evidence (Context: "room")
                            if (evidenceObj != null && contextType == "room") {
                                RoomEvidenceCard(evidenceObj = evidenceObj)
                            }

                            // DM or Group Evidence (Context: "dm" or "group") - Requirement 8
                            if (evidenceObj != null && (contextType == "dm" || contextType == "group")) {
                                ChatEvidenceCard(
                                    evidenceObj = evidenceObj,
                                    contextType = contextType
                                )
                            }

                            // Message Snapshot (Room Reports) - Requirement 3
                            if (messageSnapshotArr != null && messageSnapshotArr.length() > 0) {
                                MessageSnapshotCard(
                                    snapshotArr = messageSnapshotArr,
                                    reportedUserId = reportedUserId
                                )
                            }

                            // Speaker Log (Room Reports) - Requirement 4
                            if (speakerLogObj != null) {
                                SpeakerLogCard(speakerLogObj = speakerLogObj)
                            }

                            // Deletion Log - Requirement 5
                            if (deletionLogArr != null && deletionLogArr.length() > 0) {
                                DeletionLogCard(deletionLogArr = deletionLogArr)
                            }
                        }

                        // 5. Action Buttons (ONLY visible if claim.is_mine == true and report not resolved)
                        if (isMine && !isResolved) {
                            Text("Enforce Moderation Action", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = DarkText)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        selectedActionType = "warning"
                                        showActionDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Warning (1h)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        selectedActionType = "mute"
                                        showActionDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Mute", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        selectedActionType = "suspend"
                                        showActionDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Suspend", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showDismissDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Dismiss Report", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { showMessageUserDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Message User", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // 6. Reopens & Challenges Section
                        if (reopensArr.length() > 0) {
                            Text("Reopen & Dispute History", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = DarkText)

                            for (i in 0 until reopensArr.length()) {
                                val ro = reopensArr.optJSONObject(i) ?: continue
                                val roId = ro.optString("id")
                                val roBrief = ro.optString("reopen_brief")
                                val roStatus = ro.optString("status", "open")
                                val originalAdmin = ro.optString("original_admin")
                                val chBrief = ro.optString("challenge_brief").takeIf { it.isNotBlank() && it != "null" }
                                val decision = ro.optString("decision").takeIf { it.isNotBlank() && it != "null" }
                                val decisionNote = ro.optString("decision_note").takeIf { it.isNotBlank() && it != "null" }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Reopen Request", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFEA580C))
                                            Surface(
                                                color = when (roStatus) {
                                                    "open" -> Color(0xFFFFFBEB)
                                                    "challenged" -> Color(0xFFFEF2F2)
                                                    else -> Color(0xFFECFDF5)
                                                },
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = roStatus.uppercase(),
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (roStatus) {
                                                        "open" -> Color(0xFFB45309)
                                                        "challenged" -> Color(0xFFB91C1C)
                                                        else -> Color(0xFF15803D)
                                                    },
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Text(text = "Brief: $roBrief", fontSize = 12.5.sp, color = DarkText, lineHeight = 17.sp)

                                        if (chBrief != null) {
                                            Surface(color = Color(0xFFFFF1F2), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Text("Original Admin Challenge Brief:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF9F1239))
                                                    Text(chBrief, fontSize = 12.sp, color = Color(0xFF881337))
                                                }
                                            }
                                        }

                                        if (decision != null) {
                                            Surface(color = Color(0xFFF0FDF4), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Text("Owner Final Decision: ${decision.replace('_', ' ').uppercase()}", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color(0xFF166534))
                                                    if (!decisionNote.isNullOrBlank()) {
                                                        Text("Note: $decisionNote", fontSize = 12.sp, color = Color(0xFF14532D))
                                                    }
                                                }
                                            }
                                        }

                                        val isOriginalAdmin = currentAdminUid != null && originalAdmin.equals(currentAdminUid, ignoreCase = true)
                                        if (roStatus == "open" && isOriginalAdmin) {
                                            Button(
                                                onClick = {
                                                    activeChallengeReopenId = roId
                                                    showChallengeDialog = true
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Challenge Reopen (Notify Owner)", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                            }
                                        }

                                        if (isSuperAdmin && roStatus in listOf("open", "challenged")) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(
                                                    onClick = {
                                                        activeDisputeReopenId = roId
                                                        showDisputeDecisionDialog = true
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA)),
                                                    shape = RoundedCornerShape(10.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("Owner: Decide Dispute", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 7. Audit Event Timeline (Requirement 7: Human readable sentences)
                        if (eventsArr.length() > 0) {
                            Text("Audit Event Timeline", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = DarkText)

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    for (i in 0 until eventsArr.length()) {
                                        val ev = eventsArr.optJSONObject(i) ?: continue
                                        val evName = ev.optString("event", "event")
                                        val evTime = ev.optString("created_at").take(19).replace('T', ' ')
                                        val evDetail = ev.optJSONObject("detail")

                                        val readableSentence = formatAuditEventSentence(evName, evDetail)

                                        Row(verticalAlignment = Alignment.Top) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFD97706))
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                                    Text(
                                                        text = readableSentence,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = DarkText
                                                    )
                                                    Text(text = evTime, fontSize = 10.5.sp, color = GraySubtitle)
                                                }
                                            }
                                        }
                                        if (i < eventsArr.length() - 1) {
                                            HorizontalDivider(color = Color(0xFFF1F5F9))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }

    // Image Preview Modal
    if (previewImageUrl != null) {
        Dialog(onDismissRequest = { previewImageUrl = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { previewImageUrl = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = previewImageUrl,
                    contentDescription = "Enlarged preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }

    // Modal Action Dialog (Warning, Mute, Suspend)
    if (showActionDialog) {
        val targetUid = detailJson?.optJSONObject("reported")?.optString("user_id")
            ?: detailJson?.optJSONObject("report")?.optString("reported_user_id")
            ?: ""

        AlertDialog(
            onDismissRequest = { if (!isSubmittingAction) showActionDialog = false },
            title = {
                Text(
                    text = "Apply ${selectedActionType.replaceFirstChar { it.uppercase() }}",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Target User ID: ${targetUid.take(12)}...",
                        fontSize = 12.sp,
                        color = GraySubtitle
                    )

                    if (selectedActionType == "suspend") {
                        Text(
                            text = "Duration: $suspendHours hour(s) (1 to 90 hours allowed)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Slider(
                            value = suspendHours.toFloat(),
                            onValueChange = { suspendHours = it.toInt() },
                            valueRange = 1f..90f,
                            steps = 88
                        )
                    } else if (selectedActionType == "warning") {
                        Text(
                            text = "Warning will remain active for 1 hour.",
                            fontSize = 12.5.sp,
                            color = Color(0xFF475569)
                        )
                    } else if (selectedActionType == "mute") {
                        Text(
                            text = "Mute restricts voice and messaging indefinitely until revoked.",
                            fontSize = 12.5.sp,
                            color = Color(0xFF475569)
                        )
                    }

                    OutlinedTextField(
                        value = actionReasonInput,
                        onValueChange = { actionReasonInput = it },
                        placeholder = { Text("Reason for action (Optional)", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmittingAction = true
                        scope.launch(Dispatchers.IO) {
                            val dur = if (selectedActionType == "suspend") suspendHours else if (selectedActionType == "warning") 1 else null
                            val res = ModerationManager.applyModerationAction(
                                context = context,
                                reportId = reportId,
                                targetUserId = targetUid,
                                actionType = selectedActionType,
                                durationHours = dur,
                                reason = actionReasonInput.ifBlank { null }
                            )
                            withContext(Dispatchers.Main) {
                                isSubmittingAction = false
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Action successfully applied", Toast.LENGTH_SHORT).show()
                                    showActionDialog = false
                                    loadDetail()
                                    onReportUpdated()
                                } else {
                                    Toast.makeText(context, res.exceptionOrNull()?.message ?: "Action failed", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    enabled = !isSubmittingAction
                ) {
                    Text(if (isSubmittingAction) "Enforcing..." else "Confirm Action")
                }
            },
            dismissButton = {
                TextButton(onClick = { showActionDialog = false }, enabled = !isSubmittingAction) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Dismiss Dialog
    if (showDismissDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDismissing) showDismissDialog = false },
            title = { Text("Dismiss Report", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure no moderation action is required for this report? The reporter will be notified that no violation was found.", fontSize = 13.sp, color = Color(0xFF475569))
                    OutlinedTextField(
                        value = dismissReasonInput,
                        onValueChange = { dismissReasonInput = it },
                        placeholder = { Text("Optional dismissal note for reporter", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isDismissing = true
                        scope.launch(Dispatchers.IO) {
                            val res = ModerationManager.dismissReport(context, reportId, dismissReasonInput.ifBlank { null })
                            withContext(Dispatchers.Main) {
                                isDismissing = false
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Report dismissed", Toast.LENGTH_SHORT).show()
                                    showDismissDialog = false
                                    loadDetail()
                                    onReportUpdated()
                                } else {
                                    Toast.makeText(context, res.exceptionOrNull()?.message ?: "Dismiss failed", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                    enabled = !isDismissing
                ) {
                    Text(if (isDismissing) "Dismissing..." else "Dismiss Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDismissDialog = false }, enabled = !isDismissing) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Message User Dialog (Opens full support chat for reported user)
    if (showMessageUserDialog) {
        val targetUid = detailJson?.optJSONObject("reported")?.optString("user_id")
            ?: detailJson?.optJSONObject("report")?.optString("reported_user_id")
            ?: ""

        Dialog(
            onDismissRequest = { showMessageUserDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.92f)
                    .clip(RoundedCornerShape(20.dp)),
                color = Color(0xFFF9FAFB)
            ) {
                AdminReportSupportChatView(
                    reportedUserId = targetUid,
                    reportId = reportId,
                    onClose = { showMessageUserDialog = false }
                )
            }
        }
    }

    // Modal Reopen Report Dialog
    if (showReopenDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSubmittingReopen) showReopenDialog = false },
            title = { Text("Reopen Report", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Reopening lifts the active restriction, restores the user's safety score penalty, and returns the report to the Free queue. You must provide a brief explaining why (10 to 2000 chars).",
                        fontSize = 12.5.sp,
                        color = Color(0xFF475569),
                        lineHeight = 17.sp
                    )
                    OutlinedTextField(
                        value = reopenBriefInput,
                        onValueChange = { reopenBriefInput = it },
                        placeholder = { Text("Explain reason for reopening (min 10 characters)...", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmittingReopen = true
                        scope.launch(Dispatchers.IO) {
                            val res = ModerationManager.reopenReport(context, reportId, reopenBriefInput)
                            withContext(Dispatchers.Main) {
                                isSubmittingReopen = false
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Report reopened successfully", Toast.LENGTH_SHORT).show()
                                    showReopenDialog = false
                                    loadDetail()
                                    onReportUpdated()
                                } else {
                                    Toast.makeText(context, res.exceptionOrNull()?.message ?: "Reopen failed", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                    enabled = !isSubmittingReopen
                ) {
                    Text(if (isSubmittingReopen) "Reopening..." else "Reopen Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReopenDialog = false }, enabled = !isSubmittingReopen) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Challenge Reopen Dialog
    if (showChallengeDialog && activeChallengeReopenId != null) {
        AlertDialog(
            onDismissRequest = { if (!isSubmittingChallenge) showChallengeDialog = false },
            title = { Text("Challenge Reopen Request", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "As the original investigating admin, you can challenge this reopen. Your brief goes directly to the owner by email.",
                        fontSize = 12.5.sp,
                        color = Color(0xFF991B1B),
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = challengeBriefInput,
                        onValueChange = { challengeBriefInput = it },
                        placeholder = { Text("Provide statement for owner review (min 10 chars)...", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmittingChallenge = true
                        scope.launch(Dispatchers.IO) {
                            val res = ModerationManager.challengeReopen(context, activeChallengeReopenId!!, challengeBriefInput)
                            withContext(Dispatchers.Main) {
                                isSubmittingChallenge = false
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Challenge submitted to owner", Toast.LENGTH_SHORT).show()
                                    showChallengeDialog = false
                                    loadDetail()
                                    onReportUpdated()
                                } else {
                                    Toast.makeText(context, res.exceptionOrNull()?.message ?: "Challenge failed", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                    enabled = !isSubmittingChallenge
                ) {
                    Text(if (isSubmittingChallenge) "Sending..." else "Submit Challenge")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChallengeDialog = false }, enabled = !isSubmittingChallenge) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Owner Decision Dialog
    if (showDisputeDecisionDialog && activeDisputeReopenId != null) {
        var selectedDecision by remember { mutableStateOf("reopen_upheld") } // "reopen_upheld" | "original_upheld"

        AlertDialog(
            onDismissRequest = { if (!isSubmittingDecision) showDisputeDecisionDialog = false },
            title = { Text("Owner Final Dispute Decision", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("As platform owner, make a final binding decision on this reopen dispute:", fontSize = 12.5.sp, color = Color(0xFF475569))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedDecision == "reopen_upheld",
                            onClick = { selectedDecision = "reopen_upheld" }
                        )
                        Text("Uphold Reopen (Keep report open for review)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedDecision == "original_upheld",
                            onClick = { selectedDecision = "original_upheld" }
                        )
                        Text("Uphold Original Action (Re-enforce penalty)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    OutlinedTextField(
                        value = disputeNoteInput,
                        onValueChange = { disputeNoteInput = it },
                        placeholder = { Text("Optional owner note for admins", fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmittingDecision = true
                        scope.launch(Dispatchers.IO) {
                            val res = ModerationManager.ownerDecideDispute(
                                context = context,
                                reopenId = activeDisputeReopenId!!,
                                decision = selectedDecision,
                                note = disputeNoteInput.ifBlank { null }
                            )
                            withContext(Dispatchers.Main) {
                                isSubmittingDecision = false
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Dispute decided successfully", Toast.LENGTH_SHORT).show()
                                    showDisputeDecisionDialog = false
                                    loadDetail()
                                    onReportUpdated()
                                } else {
                                    Toast.makeText(context, res.exceptionOrNull()?.message ?: "Decision failed", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA)),
                    enabled = !isSubmittingDecision
                ) {
                    Text(if (isSubmittingDecision) "Saving..." else "Submit Decision")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisputeDecisionDialog = false }, enabled = !isSubmittingDecision) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ----------------------------------------------------
// PROOF RENDERING HELPER COMPOSABLES
// ----------------------------------------------------

@Composable
private fun EvidenceStatusBadge(status: String) {
    val bg = when (status.lowercase()) {
        "server_verified" -> Color(0xFFDCFCE7)
        "unverified" -> Color(0xFFFEF3C7)
        else -> Color(0xFFF1F5F9)
    }
    val fg = when (status.lowercase()) {
        "server_verified" -> Color(0xFF15803D)
        "unverified" -> Color(0xFFB45309)
        else -> Color(0xFF64748B)
    }
    val text = when (status.lowercase()) {
        "server_verified" -> "Verified by server"
        "unverified" -> "Sent by the reporter's app (not verified)"
        else -> "No proof attached"
    }

    Surface(color = bg, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun MomentEvidenceCard(evidenceObj: JSONObject, onImageClick: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Moment Evidence", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkText)

            val repMoment = evidenceObj.optJSONObject("reported_moment")
            if (repMoment != null) {
                Surface(
                    color = Color(0xFFFFFBEB),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Reported Moment", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFB45309))
                            Text(repMoment.optString("created_at").take(10), fontSize = 11.sp, color = Color(0xFFD97706))
                        }
                        val content = repMoment.optString("content")
                        if (content.isNotBlank()) {
                            Text(content, fontSize = 13.sp, color = DarkText)
                        }
                        val imageUrls = repMoment.optJSONArray("image_urls")
                        if (imageUrls != null && imageUrls.length() > 0) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (i in 0 until imageUrls.length()) {
                                    val url = imageUrls.optString(i) ?: continue
                                    AsyncImage(
                                        model = url,
                                        contentDescription = "Moment image",
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onImageClick(url) },
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val recentArr = evidenceObj.optJSONArray("recent_moments")
            if (recentArr != null && recentArr.length() > 0) {
                Text("Their Last Moments", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = GraySubtitle)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 0 until recentArr.length()) {
                        val m = recentArr.optJSONObject(i) ?: continue
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(m.optString("content", "No text"), fontSize = 12.sp, color = DarkText)
                                val imgArr = m.optJSONArray("image_urls")
                                if (imgArr != null && imgArr.length() > 0) {
                                    Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        for (j in 0 until imgArr.length()) {
                                            val u = imgArr.optString(j) ?: continue
                                            AsyncImage(
                                                model = u,
                                                contentDescription = "Recent moment image",
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .clickable { onImageClick(u) },
                                                contentScale = ContentScale.Crop
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
private fun RoomEvidenceCard(evidenceObj: JSONObject) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Voice Room Evidence", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkText)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Room Title:", fontSize = 12.sp, color = GraySubtitle)
                Text(evidenceObj.optString("title", "Voice Room"), fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, color = DarkText)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Room Status:", fontSize = 12.sp, color = GraySubtitle)
                Text(if (evidenceObj.optBoolean("room_live")) "LIVE" else "ENDED", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (evidenceObj.optBoolean("room_live")) Color(0xFF16A34A) else Color(0xFF64748B))
            }
            if (evidenceObj.has("captured_at")) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Captured At:", fontSize = 12.sp, color = GraySubtitle)
                    Text(evidenceObj.optString("captured_at").take(19).replace('T', ' '), fontSize = 11.5.sp, color = DarkText)
                }
            }
        }
    }
}

@Composable
private fun ChatEvidenceCard(evidenceObj: JSONObject, contextType: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (contextType == "group") "Group Chat Evidence" else "1:1 Chat Evidence", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkText)

            val messagesArr = evidenceObj.optJSONArray("messages")
            if (messagesArr == null || messagesArr.length() == 0) {
                Text("No chat messages contained in evidence.", fontSize = 12.sp, color = GraySubtitle)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 0 until messagesArr.length()) {
                        val msg = messagesArr.optJSONObject(i) ?: continue
                        val sender = msg.optString("sender", "unknown")
                        val text = msg.optString("text").takeIf { it.isNotBlank() && it != "null" } ?: "[${msg.optString("type", "message")}]"
                        val sentAt = msg.optString("sent_at").take(19).replace('T', ' ')

                        Surface(
                            color = if (sender == "them" || sender.contains("reported", ignoreCase = true)) Color(0xFFFFF2F2) else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (sender == "them" || sender.contains("reported", ignoreCase = true)) Color(0xFFFECACA) else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(sender.uppercase(), fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = Color(0xFF475569))
                                    Text(sentAt, fontSize = 10.sp, color = GraySubtitle)
                                }
                                Text(text, fontSize = 12.5.sp, color = DarkText, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageSnapshotCard(snapshotArr: JSONArray, reportedUserId: String?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Room Message Snapshot", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkText)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 0 until snapshotArr.length()) {
                    val item = snapshotArr.optJSONObject(i) ?: continue
                    val userId = item.optString("user_id")
                    val senderName = item.optString("sender", "User")
                    val msgText = item.optString("message")
                    val createdAt = item.optString("created_at").take(19).replace('T', ' ')

                    val isReportedUser = reportedUserId != null && userId.equals(reportedUserId, ignoreCase = true)

                    Surface(
                        color = if (isReportedUser) Color(0xFFFEF2F2) else Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isReportedUser) Color(0xFFFCA5A5) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(senderName, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = DarkText)
                                    if (isReportedUser) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(color = Color(0xFFDC2626), shape = RoundedCornerShape(4.dp)) {
                                            Text(
                                                "REPORTED USER",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(createdAt, fontSize = 10.sp, color = GraySubtitle)
                            }
                            Text(msgText, fontSize = 12.5.sp, color = DarkText, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeakerLogCard(speakerLogObj: JSONObject) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Voice Room Speaker Log", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkText)

            val repUserObj = speakerLogObj.optJSONObject("reported_user")
            if (repUserObj != null) {
                val summary = repUserObj.optJSONObject("summary")
                val micTurns = summary?.optInt("mic_turns") ?: 0
                val micSecs = summary?.optInt("mic_on_seconds") ?: 0
                val formattedTime = String.format(Locale.US, "%02d:%02d", micSecs / 60, micSecs % 60)

                Surface(color = Color(0xFFFFFBEB), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Color(0xFFFDE68A)), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Mic Turns: $micTurns", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFB45309))
                        Text("Mic Time: $formattedTime", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFB45309))
                    }
                }

                val eventsArr = repUserObj.optJSONArray("events")
                if (eventsArr != null && eventsArr.length() > 0) {
                    Text("Speaker Timeline", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = GraySubtitle)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (i in 0 until eventsArr.length()) {
                            val ev = eventsArr.optJSONObject(i) ?: continue
                            val atTime = ev.optString("at").take(19).replace('T', ' ')
                            val eventName = ev.optString("event")
                            val role = ev.optString("role")
                            Text("• $atTime - $eventName ($role)", fontSize = 11.5.sp, color = DarkText)
                        }
                    }
                }
            }

            val participantsArr = speakerLogObj.optJSONArray("participants")
            if (participantsArr != null && participantsArr.length() > 0) {
                Text("Room Participants", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = GraySubtitle)
                val names = mutableListOf<String>()
                for (i in 0 until participantsArr.length()) {
                    val p = participantsArr.optJSONObject(i) ?: continue
                    names.add(p.optString("name", "User"))
                }
                Text(names.joinToString(", "), fontSize = 11.5.sp, color = Color(0xFF475569))
            }
        }
    }
}

@Composable
private fun DeletionLogCard(deletionLogArr: JSONArray) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Messages Deleted by Reporter (${deletionLogArr.length()})",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFFDC2626)
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 0 until deletionLogArr.length()) {
                    val item = deletionLogArr.optJSONObject(i) ?: continue
                    val atTime = item.optString("at").take(19).replace('T', ' ')
                    val msgType = item.optString("type", "message")
                    Text("• $atTime - Deleted $msgType message", fontSize = 12.sp, color = Color(0xFF7F1D1D))
                }
            }
        }
    }
}

// ----------------------------------------------------
// UTILITY HELPERS
// ----------------------------------------------------

private fun parseJsonObject(obj: Any?): JSONObject? {
    if (obj == null || obj == JSONObject.NULL) return null
    if (obj is JSONObject) return obj
    if (obj is String && obj.isNotBlank()) {
        return try {
            JSONObject(obj)
        } catch (_: Exception) {
            null
        }
    }
    return null
}

private fun parseJsonArray(obj: Any?): JSONArray? {
    if (obj == null || obj == JSONObject.NULL) return null
    if (obj is JSONArray) return obj
    if (obj is String && obj.isNotBlank()) {
        return try {
            JSONArray(obj)
        } catch (_: Exception) {
            null
        }
    }
    return null
}

private fun formatAuditEventSentence(evName: String, detailObj: JSONObject?): String {
    val norm = evName.lowercase().trim()
    return when {
        norm.contains("take_charge") -> "Admin took charge"
        norm.contains("auto_release") -> "Auto released after 48 hours"
        norm.contains("release") -> "Admin released claim"
        norm.contains("dismiss") -> "Report dismissed"
        norm.contains("reopen") -> "Report reopened"
        norm.contains("challenge") -> "Admin challenged reopen"
        norm.contains("owner") || norm.contains("dispute") -> "Owner decided dispute"
        norm.contains("action") || norm.contains("warning") || norm.contains("mute") || norm.contains("suspend") -> {
            val actType = detailObj?.optString("action_type") ?: norm.replace("action_", "")
            "Action: ${actType.ifBlank { "enforced" }}"
        }
        else -> evName.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
}

private fun formatCountdown(expiresAt: String?): String {
    if (expiresAt.isNullOrBlank()) return "00:00:00"
    return try {
        val target = Instant.parse(expiresAt)
        val now = Instant.now()
        val duration = Duration.between(now, target)
        if (duration.isNegative || duration.isZero) {
            "Expired"
        } else {
            val hours = duration.toHours()
            val minutes = (duration.toMinutes() % 60)
            val seconds = (duration.seconds % 60)
            String.format(Locale.US, "%02dh %02dm %02ds", hours, minutes, seconds)
        }
    } catch (_: Exception) {
        "Active"
    }
}

@Composable
private fun ReporterProofScreenshotsSection(
    mediaPathsArr: JSONArray?,
    onImageClick: (String) -> Unit
) {
    if (mediaPathsArr == null || mediaPathsArr.length() == 0) return

    val context = LocalContext.current
    var signedUrls by remember { mutableStateOf<List<String>>(emptyList()) }
    var isGenerating by remember { mutableStateOf(true) }

    LaunchedEffect(mediaPathsArr) {
        isGenerating = true
        val urls = mutableListOf<String>()
        val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
        for (i in 0 until mediaPathsArr.length()) {
            val rawPath = mediaPathsArr.optString(i).takeIf { it.isNotBlank() && it != "null" } ?: continue
            val cleanPath = rawPath.removePrefix("/")
            if (token != null) {
                try {
                    val res = SupabaseClient.api.createSignedUrl(
                        auth = token,
                        bucket = "report-proofs",
                        filename = cleanPath,
                        body = CreateSignedUrlRequest(expiresIn = 300)
                    )
                    if (res.isSuccessful && res.body() != null) {
                        val resolved = res.body()!!.getResolvedPath()
                        if (resolved != null) {
                            val fullUrl = if (resolved.startsWith("http")) resolved else "${BuildConfig.SUPABASE_URL}/storage/v1$resolved"
                            urls.add(fullUrl)
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        signedUrls = urls
        isGenerating = false
    }

    if (!isGenerating && signedUrls.isNotEmpty()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Screenshots from the reporter",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = DarkText
                )

                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(signedUrls.size) { index ->
                        val url = signedUrls[index]
                        AsyncImage(
                            model = url,
                            contentDescription = "Reporter proof screenshot",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(width = 140.dp, height = 180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                .clickable { onImageClick(url) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminReportSupportChatView(
    reportedUserId: String,
    reportId: String,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val messages by AdminSupportManager.messages.collectAsState()
    val conversations by AdminSupportManager.conversations.collectAsState()
    val isSending by AdminSupportManager.isSending.collectAsState()

    var inputText by remember { mutableStateOf("") }
    var selectedOpenHours by remember { mutableIntStateOf(2) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    val targetConv = remember(conversations, reportedUserId) {
        conversations.find { it.user_id == reportedUserId }
    }

    LaunchedEffect(reportedUserId) {
        if (targetConv != null) {
            AdminSupportManager.selectConversation(context, targetConv)
        } else {
            AdminSupportManager.fetchConversations(context)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val statusLine = AdminSupportManager.formatChatOpenStatus(targetConv?.chat_open_until)
    val isOpen = statusLine.startsWith("Chat open until")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFB))
    ) {
        // Top Header
        Surface(
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF3F4F6))
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Close",
                        tint = Color(0xFF374151),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Message Reported User",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = DarkText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isOpen) Color(0xFF16A34A) else Color(0xFF94A3B8))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusLine,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOpen) Color(0xFF15803D) else Color(0xFF64748B)
                        )
                    }
                }

                if (isOpen) {
                    OutlinedButton(
                        onClick = {
                            AdminSupportManager.adminCloseSupportChat(
                                context = context,
                                userId = reportedUserId,
                                onSuccess = {
                                    Toast.makeText(context, "Support chat closed", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Close chat", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Message List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Send an inquiry to open a 1:1 support window with the user.",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                val distinctMsgs = remember(messages) { messages.distinctBy { it.id } }

                androidx.compose.foundation.lazy.LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (i in distinctMsgs.indices) {
                        val msg = distinctMsgs[i]
                        val prevMsg = if (i > 0) distinctMsgs[i - 1] else null

                        val msgDateHeader = AdminSupportManager.formatDateHeader(msg.created_at)
                        val prevDateHeader = if (prevMsg != null) AdminSupportManager.formatDateHeader(prevMsg.created_at) else null

                        if (i == 0 || msgDateHeader != prevDateHeader) {
                            item(key = "date_hdr_$i") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        color = Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = msgDateHeader,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF475569),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }

                        item(key = msg.id) {
                            val isAdminMsg = msg.sender_type == "admin"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isAdminMsg) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isAdminMsg) GoldPrimary else Color.White,
                                    border = BorderStroke(1.dp, if (isAdminMsg) Color(0xFFF59E0B) else Color(0xFFE2E8F0)),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = msg.body,
                                            fontSize = 13.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = AdminSupportManager.formatRelativeTime(msg.created_at),
                                            fontSize = 10.sp,
                                            color = if (isAdminMsg) Color(0xFF451A03) else Color(0xFF94A3B8),
                                            modifier = Modifier.align(Alignment.End)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Input Box
        Surface(
            color = Color.White,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Keep chat open:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedOpenHours == 1,
                            onClick = { selectedOpenHours = 1 },
                            label = { Text("1 hour", fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = Color.Black
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(30.dp)
                        )

                        FilterChip(
                            selected = selectedOpenHours == 2,
                            onClick = { selectedOpenHours = 2 },
                            label = { Text("2 hours (Default)", fontSize = 11.5.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = GoldPrimary,
                                selectedLabelColor = Color.Black
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Type inquiry regarding report...", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_report_message_input"),
                        shape = RoundedCornerShape(20.dp),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isSending) {
                                val textToSend = inputText
                                inputText = ""
                                AdminSupportManager.adminSendMessage(
                                    context = context,
                                    userId = reportedUserId,
                                    messageText = textToSend,
                                    reportId = reportId,
                                    openHours = selectedOpenHours,
                                    onSuccess = {
                                        Toast.makeText(context, "Inquiry sent to user", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { err ->
                                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                    }
                                )
                            }
                        },
                        enabled = inputText.isNotBlank() && !isSending,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank() && !isSending) GoldPrimary else Color(0xFFE2E8F0))
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send Message",
                                tint = if (inputText.isNotBlank()) Color.Black else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
