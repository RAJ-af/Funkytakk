package com.example.moderation

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ReportUserRequest
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.repository.EncryptedChatRepository
import com.example.ui.components.SoftCloseIcon
import com.example.ui.components.SoftShieldIcon
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ReportUserDialog"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportUserDialog(
    reportedUserId: String,
    reportedUserName: String,
    contextType: String, // "profile", "dm", "moment", "room"
    contextId: String? = null,
    dmProofMessages: List<Map<String, Any?>>? = null,
    dmDeletionLog: List<Map<String, Any?>>? = null,
    existingScreenshotUrl: String? = null,
    onDismiss: () -> Unit,
    onReportSubmitted: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val categories = remember {
        listOf(
            "Spam & Scam",
            "Hate Speech",
            "Harassment",
            "Inappropriate Content / NSFW",
            "Other"
        )
    }

    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var reasonText by remember { mutableStateOf("") }
    var includeProof by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    val showProofSwitch = (contextType == "dm" || contextType == "group") && !dmProofMessages.isNullOrEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2)),
                        contentAlignment = Alignment.Center
                    ) {
                        SoftShieldIcon(size = 18.dp, color = Color(0xFFEF4444))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Report ${contextType.replaceFirstChar { it.uppercase() }}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Reporting $reportedUserName",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                ) {
                    SoftCloseIcon(size = 14.dp, color = Color(0xFF475569))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Select a reason for this report",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF334155)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Categories
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categories) { cat ->
                    val isSel = selectedCategory == cat
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) Color(0xFFFFFBEB) else Color(0xFFF8FAFC))
                            .border(
                                width = if (isSel) 1.5.dp else 1.dp,
                                color = if (isSel) GoldAccent else Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSel,
                            onClick = { selectedCategory = cat },
                            colors = RadioButtonDefaults.colors(selectedColor = GoldAccent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = cat,
                            fontSize = 13.5.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Additional details input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Additional Details (Optional)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )
                Text(
                    text = "${reasonText.length}/1000",
                    fontSize = 11.sp,
                    color = if (reasonText.length > 1000) Color(0xFFEF4444) else Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(
                        BorderStroke(
                            1.dp,
                            if (reasonText.length > 1000) Color(0xFFEF4444) else Color(0xFFE2E8F0)
                        ),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.TopStart
            ) {
                if (reasonText.isEmpty()) {
                    Text(
                        text = "Provide any helpful context for moderators...",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                }
                BasicTextField(
                    value = reasonText,
                    onValueChange = { if (it.length <= 1000) reasonText = it },
                    textStyle = TextStyle(color = Color.Black, fontSize = 13.sp),
                    cursorBrush = SolidColor(Color.Black),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (showProofSwitch) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Include the last 20 messages as proof",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Only messages you can see are sent, to moderators only.",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = includeProof,
                        onCheckedChange = { includeProof = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = GoldAccent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Button
            Button(
                onClick = {
                    if (reasonText.length > 1000) {
                        Toast.makeText(context, mapReportError("reason_too_long"), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isSubmitting = true
                    val evidenceMap: Map<String, Any?>? = if (showProofSwitch && includeProof && !dmProofMessages.isNullOrEmpty()) {
                        mapOf("messages" to dmProofMessages)
                    } else null

                    val deletionLogList: List<Map<String, Any?>>? = if (showProofSwitch && includeProof && !dmDeletionLog.isNullOrEmpty()) {
                        dmDeletionLog
                    } else null

                    scope.launch {
                        submitReport(
                            context = context,
                            reportedUserId = reportedUserId,
                            contextType = contextType,
                            contextId = contextId,
                            category = selectedCategory,
                            reason = reasonText.ifBlank { null },
                            evidence = evidenceMap,
                            deletionLog = deletionLogList,
                            existingScreenshotUrl = existingScreenshotUrl,
                            onSuccess = {
                                isSubmitting = false
                                Toast.makeText(context, "Thanks, our team will review your report.", Toast.LENGTH_LONG).show()
                                onReportSubmitted()
                                onDismiss()
                            },
                            onError = { err ->
                                isSubmitting = false
                                Toast.makeText(context, mapReportError(err), Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submitting Report...", color = Color.White, fontWeight = FontWeight.Bold)
                } else {
                    SoftShieldIcon(size = 18.dp, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit Report", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

private suspend fun submitReport(
    context: Context,
    reportedUserId: String,
    contextType: String,
    contextId: String?,
    category: String,
    reason: String?,
    evidence: Map<String, Any?>?,
    deletionLog: List<Map<String, Any?>>?,
    existingScreenshotUrl: String?,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
            ?: return@withContext onError("not_authenticated")

        // Ensure reportedUserId is a valid Supabase Auth UUID string, not a display name or Firebase UID
        val effectiveReportedUserId = if (SupabaseSessionManager.isValidUuid(reportedUserId)) {
            reportedUserId.trim()
        } else {
            val resolved = EncryptedChatRepository.resolveRecipientAuthUserId(context, reportedUserId)
            if (!resolved.isNullOrBlank() && SupabaseSessionManager.isValidUuid(resolved)) {
                resolved.trim()
            } else {
                reportedUserId.trim()
            }
        }

        Log.d(
            TAG,
            "Calling report_user: target=$effectiveReportedUserId, context=$contextType, category=$category, contextId=$contextId"
        )

        val resp = SupabaseClient.api.reportUser(
            apiKey = BuildConfig.SUPABASE_ANON_KEY,
            auth = token,
            body = ReportUserRequest(
                p_reported_user_id = effectiveReportedUserId,
                p_context_type = contextType,
                p_category = category,
                p_reason = reason,
                p_context_id = contextId,
                p_screenshot_url = existingScreenshotUrl,
                p_evidence = evidence,
                p_deletion_log = deletionLog
            )
        )

        if (resp.isSuccessful) {
            Log.i(TAG, "report_user RPC succeeded: HTTP ${resp.code()}")
            withContext(Dispatchers.Main) { onSuccess() }
        } else {
            val code = resp.code()
            val rawErr = resp.errorBody()?.string().orEmpty()
            Log.e(TAG, "report_user RPC failed with HTTP $code: $rawErr")
            withContext(Dispatchers.Main) {
                onError("[$code] $rawErr")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "report_user exception: ${e.message}", e)
        withContext(Dispatchers.Main) { onError(e.localizedMessage ?: "Could not send, try again.") }
    }
}

fun mapReportError(raw: String?): String {
    if (raw.isNullOrBlank()) return "Could not send, try again."
    val lower = raw.lowercase()

    // Debugging assistance: extract HTTP status code or error code if present
    val codePrefix = if (raw.startsWith("[")) {
        val closing = raw.indexOf("]")
        if (closing > 0) raw.substring(0, closing + 1) + " " else ""
    } else ""

    val friendlyMsg = when {
        lower.contains("not_authenticated") -> "Please log in to submit a report."
        lower.contains("cannot_report_yourself") -> "You cannot report yourself."
        lower.contains("category_required") -> "Please select a category."
        lower.contains("reason_too_long") -> "Reason text is too long (max 1000 characters)."
        lower.contains("evidence_too_large") -> "Evidence size exceeds limit (max 200 KB). Retry without proof."
        lower.contains("deletion_log_too_large") -> "Deletion log size exceeds limit (max 20 KB)."
        lower.contains("rate_limited") -> "You can send up to 10 reports per hour. Please wait a while before reporting again."
        lower.contains("not_in_room") -> "You are not in this voice room."
        lower.contains("reported_not_in_room") -> "The reported user is not in this voice room."
        lower.contains("moment_mismatch") -> "The reported moment was not found or does not belong to this user."
        lower.contains("cannot_report_own_group") -> "You can't report a group you created."
        lower.contains("function not found") || lower.contains("404") -> "Server RPC signature mismatch [404]. Check parameter types."
        else -> raw.trim()
    }

    return if (codePrefix.isNotEmpty() && !friendlyMsg.startsWith("[")) {
        "$codePrefix$friendlyMsg"
    } else {
        friendlyMsg
    }
}
