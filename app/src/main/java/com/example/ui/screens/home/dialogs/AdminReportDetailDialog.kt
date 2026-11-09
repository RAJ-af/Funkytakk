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
import com.example.AdminSendSupportMessageRequest
import com.example.BuildConfig
import com.example.ModerationManager
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
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
import java.time.Duration
import java.time.Instant

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
    var isSendingUserMessage by remember { mutableStateOf(false) }

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

                                    // Check 30 days window for reopen
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
                                    Surface(
                                        color = Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = reportObj.optString("context_type", "report").uppercase(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF475569),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
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

                                // Screenshot evidence if provided
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
                                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }

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

                                    // Safety score badge
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

                        // 4. Action Buttons (ONLY visible if claim.is_mine == true and report not resolved)
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

                        // 5. Reopens & Challenges Section
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

                                        // If I am the original admin and status is 'open': show "Challenge" button
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

                                        // If I am superadmin (owner) and status is 'open' or 'challenged': show dispute buttons
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

                        // 6. Events Timeline
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
                                        val evDetail = ev.optJSONObject("detail")?.toString()

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
                                                        text = evName.replace('_', ' ').uppercase(),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.5.sp,
                                                        color = DarkText
                                                    )
                                                    Text(text = evTime, fontSize = 10.5.sp, color = GraySubtitle)
                                                }
                                                if (!evDetail.isNullOrBlank() && evDetail != "{}") {
                                                    Text(
                                                        text = evDetail,
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF64748B),
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
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

    // Modal Message User Dialog (For investigation)
    if (showMessageUserDialog) {
        val targetUid = detailJson?.optJSONObject("reported")?.optString("user_id")
            ?: detailJson?.optJSONObject("report")?.optString("reported_user_id")
            ?: ""

        AlertDialog(
            onDismissRequest = { if (!isSendingUserMessage) showMessageUserDialog = false },
            title = { Text("Message User (Investigation)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Send a support inquiry to this user regarding this incident. They can reply directly in Support Chat.", fontSize = 13.sp, color = Color(0xFF475569))
                    OutlinedTextField(
                        value = messageUserInput,
                        onValueChange = { messageUserInput = it },
                        placeholder = { Text("Type your inquiry to the user...", fontSize = 13.sp) },
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
                        if (messageUserInput.isBlank()) {
                            Toast.makeText(context, "Please enter a message", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSendingUserMessage = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context) ?: ""
                                val resp = SupabaseClient.api.adminSendSupportMessage(
                                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                                    auth = token,
                                    body = AdminSendSupportMessageRequest(
                                        p_user_id = targetUid,
                                        p_message = messageUserInput.trim(),
                                        p_report_id = reportId
                                    )
                                )
                                withContext(Dispatchers.Main) {
                                    isSendingUserMessage = false
                                    if (resp.isSuccessful) {
                                        Toast.makeText(context, "Support message sent to user", Toast.LENGTH_SHORT).show()
                                        showMessageUserDialog = false
                                    } else {
                                        Toast.makeText(context, "Failed to send message: ${resp.code()}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isSendingUserMessage = false
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    enabled = !isSendingUserMessage
                ) {
                    Text(if (isSendingUserMessage) "Sending..." else "Send Message")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMessageUserDialog = false }, enabled = !isSendingUserMessage) {
                    Text("Cancel")
                }
            }
        )
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
                    Text(
                        text = "${reopenBriefInput.trim().length}/2000 characters",
                        fontSize = 11.sp,
                        color = if (reopenBriefInput.trim().length in 10..2000) Color(0xFF16A34A) else Color(0xFFDC2626)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = reopenBriefInput.trim()
                        if (trimmed.length < 10) {
                            Toast.makeText(context, "Brief must be at least 10 characters.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmittingReopen = true
                        scope.launch(Dispatchers.IO) {
                            val res = ModerationManager.reopenReport(context, reportId, trimmed)
                            withContext(Dispatchers.Main) {
                                isSubmittingReopen = false
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Report reopened and returned to Free queue", Toast.LENGTH_LONG).show()
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
                    enabled = !isSubmittingReopen && reopenBriefInput.trim().length >= 10
                ) {
                    Text(if (isSubmittingReopen) "Reopening..." else "Submit Reopen")
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
            title = { Text("Challenge Reopen", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = Color(0xFFFFFBEB),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Notice: Your brief goes directly to the owner by email.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color(0xFF92400E),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Text(
                        text = "Provide your counter-argument for why the original moderation action was justified (10 to 2000 chars):",
                        fontSize = 12.5.sp,
                        color = Color(0xFF475569)
                    )
                    OutlinedTextField(
                        value = challengeBriefInput,
                        onValueChange = { challengeBriefInput = it },
                        placeholder = { Text("Type counter brief...", fontSize = 13.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        maxLines = 4
                    )
                    Text(
                        text = "${challengeBriefInput.trim().length}/2000 characters",
                        fontSize = 11.sp,
                        color = if (challengeBriefInput.trim().length in 10..2000) Color(0xFF16A34A) else Color(0xFFDC2626)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = challengeBriefInput.trim()
                        if (trimmed.length < 10) {
                            Toast.makeText(context, "Brief must be at least 10 characters.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isSubmittingChallenge = true
                        scope.launch(Dispatchers.IO) {
                            val res = ModerationManager.challengeReopen(context, activeChallengeReopenId!!, trimmed)
                            withContext(Dispatchers.Main) {
                                isSubmittingChallenge = false
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Challenge submitted. Owner notified by email.", Toast.LENGTH_LONG).show()
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
                    enabled = !isSubmittingChallenge && challengeBriefInput.trim().length >= 10
                ) {
                    Text(if (isSubmittingChallenge) "Sending..." else "Challenge & Email Owner")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChallengeDialog = false }, enabled = !isSubmittingChallenge) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal Owner Decide Dispute Dialog
    if (showDisputeDecisionDialog && activeDisputeReopenId != null) {
        var selectedDecision by remember { mutableStateOf("reopen_upheld") }

        AlertDialog(
            onDismissRequest = { if (!isSubmittingDecision) showDisputeDecisionDialog = false },
            title = { Text("Owner Dispute Decision", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "As superadmin (owner), choose which party is upheld:",
                        fontSize = 13.sp,
                        color = Color(0xFF334155)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedDecision == "reopen_upheld",
                            onClick = { selectedDecision = "reopen_upheld" }
                        )
                        Text("Uphold Reopen (Lift penalty)", fontSize = 13.sp, fontWeight = FontWeight.Medium)
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
            String.format("%02dh %02dm %02ds", hours, minutes, seconds)
        }
    } catch (_: Exception) {
        "Active"
    }
}
