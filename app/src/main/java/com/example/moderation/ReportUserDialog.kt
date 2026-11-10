package com.example.moderation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.AttachReportProofsRequest
import com.example.BuildConfig
import com.example.ReportGroupRequest
import com.example.ReportUserRequest
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.repository.EncryptedChatRepository
import com.example.repository.GroupChatRepository
import com.example.ui.components.SoftCloseIcon
import com.example.ui.components.SoftShieldIcon
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.Locale

private const val TAG = "ReportSheet"

data class ReasonOption(
    val title: String,
    val icon: ImageVector
)

enum class ReportSubmitState {
    IDLE,
    UPLOADING_PROOF,
    SENDING_REPORT,
    ATTACHING_PROOF,
    UPLOAD_FAILED,
    SUCCESS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportUserDialog(
    reportedUserId: String,
    reportedUserName: String,
    contextType: String, // "profile", "dm", "group", "moment", "room"
    contextId: String? = null,
    dmProofMessages: List<Map<String, Any?>>? = null,
    dmDeletionLog: List<Map<String, Any?>>? = null,
    existingScreenshotUrl: String? = null,
    onDismiss: () -> Unit,
    onReportSubmitted: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val formattedContextTitle = remember(contextType) {
        when (contextType.lowercase()) {
            "dm", "1:1" -> "Dm"
            "group" -> "Group"
            "moment" -> "Moment"
            "profile" -> "Profile"
            else -> contextType.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
    }

    val reasonOptions = remember {
        listOf(
            ReasonOption("Spam & Scam", Icons.Default.Warning),
            ReasonOption("Hate Speech", Icons.Default.Campaign),
            ReasonOption("Harassment", Icons.Default.PersonOff),
            ReasonOption("Inappropriate Content / NSFW", Icons.Default.VisibilityOff),
            ReasonOption("Other", Icons.Default.HelpOutline)
        )
    }

    var selectedReason by remember { mutableStateOf(reasonOptions[0].title) }
    var reasonText by remember { mutableStateOf("") }
    var includeProof by remember { mutableStateOf(true) }
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    var submitState by remember { mutableStateOf(ReportSubmitState.IDLE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var attachmentWarning by remember { mutableStateOf<String?>(null) }

    val showProofSection = contextType.lowercase() != "moment"
    val showProofSwitch = (contextType.lowercase() == "dm" || contextType.lowercase() == "group") && !dmProofMessages.isNullOrEmpty()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 4)
    ) { uris ->
        if (uris.isNotEmpty()) {
            val available = 4 - selectedImageUris.size
            if (available > 0) {
                selectedImageUris = selectedImageUris + uris.take(available)
            }
        }
    }

    LaunchedEffect(submitState) {
        if (submitState == ReportSubmitState.SUCCESS) {
            delay(1500L)
            onReportSubmitted()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (submitState == ReportSubmitState.IDLE || submitState == ReportSubmitState.UPLOAD_FAILED || submitState == ReportSubmitState.SUCCESS) {
                onDismiss()
            }
        },
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2)),
                        contentAlignment = Alignment.Center
                    ) {
                        SoftShieldIcon(size = 20.dp, color = Color(0xFFEF4444))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Report $formattedContextTitle",
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
                    enabled = submitState == ReportSubmitState.IDLE || submitState == ReportSubmitState.UPLOAD_FAILED || submitState == ReportSubmitState.SUCCESS,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                ) {
                    SoftCloseIcon(size = 14.dp, color = Color(0xFF475569))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (submitState == ReportSubmitState.SUCCESS) {
                // Success Confirmation Card
                Surface(
                    color = Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Thanks, our team will review your report.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF15803D),
                            textAlign = TextAlign.Center
                        )
                        if (attachmentWarning != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = attachmentWarning!!,
                                fontSize = 12.sp,
                                color = Color(0xFFB45309),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // "What is the problem?" Section
                Text(
                    text = "What is the problem?",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    reasonOptions.forEach { opt ->
                        val isSel = selectedReason == opt.title
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedReason = opt.title }
                                .testTag("reason_card_${opt.title}"),
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSel) Color(0xFFFFFBEB) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                width = if (isSel) 1.5.dp else 1.dp,
                                color = if (isSel) GoldAccent else Color(0xFFE2E8F0)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(if (isSel) Color(0xFFFEF3C7) else Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = opt.icon,
                                        contentDescription = null,
                                        tint = if (isSel) Color(0xFFD97706) else Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = opt.title,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.weight(1f)
                                )

                                RadioButton(
                                    selected = isSel,
                                    onClick = { selectedReason = opt.title },
                                    colors = RadioButtonDefaults.colors(selectedColor = GoldAccent)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // "Brief" multi-line field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Brief (Optional)",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = "${reasonText.length}/1000",
                        fontSize = 11.5.sp,
                        color = if (reasonText.length > 1000) Color(0xFFEF4444) else Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
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

                // PROOF SECTION (Only for 1:1, profile and group; NOT for moments)
                if (showProofSection) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add proof (optional)",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                        Text(
                            text = "${selectedImageUris.size}/4",
                            fontSize = 11.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Dashed "Add screenshots" tile
                        if (selectedImageUris.size < 4) {
                            item {
                                Surface(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.5.dp, GoldAccent)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = "Add screenshot",
                                            tint = GoldAccent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Add",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }

                        // Image Thumbnails with remove (x) button
                        items(selectedImageUris, key = { it.toString() }) { uri ->
                            Box(modifier = Modifier.size(72.dp)) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Proof thumbnail",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                )
                                IconButton(
                                    onClick = { selectedImageUris = selectedImageUris - uri },
                                    modifier = Modifier
                                        .size(22.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFDC2626))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove screenshot",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Switch for 1:1 and group proof messages
                if (showProofSwitch) {
                    Spacer(modifier = Modifier.height(14.dp))
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

                // Reassurance Line with Lock Icon
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Your report is private. The other person is never told who reported.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }

                // Upload failure retry banner
                if (submitState == ReportSubmitState.UPLOAD_FAILED) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Could not upload the screenshots",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFDC2626)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        executeReportSubmit(
                                            context = context,
                                            scope = scope,
                                            reportedUserId = reportedUserId,
                                            reportedUserName = reportedUserName,
                                            contextType = contextType,
                                            contextId = contextId,
                                            category = selectedReason,
                                            reason = reasonText,
                                            includeProof = includeProof,
                                            dmProofMessages = dmProofMessages,
                                            dmDeletionLog = dmDeletionLog,
                                            imageUris = selectedImageUris,
                                            existingScreenshotUrl = existingScreenshotUrl,
                                            skipProofUpload = false,
                                            onStateChange = { submitState = it },
                                            onWarning = { attachmentWarning = it },
                                            onError = { err ->
                                                errorMessage = mapReportError(err)
                                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Retry Upload", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        executeReportSubmit(
                                            context = context,
                                            scope = scope,
                                            reportedUserId = reportedUserId,
                                            reportedUserName = reportedUserName,
                                            contextType = contextType,
                                            contextId = contextId,
                                            category = selectedReason,
                                            reason = reasonText,
                                            includeProof = includeProof,
                                            dmProofMessages = dmProofMessages,
                                            dmDeletionLog = dmDeletionLog,
                                            imageUris = selectedImageUris,
                                            existingScreenshotUrl = existingScreenshotUrl,
                                            skipProofUpload = true,
                                            onStateChange = { submitState = it },
                                            onWarning = { attachmentWarning = it },
                                            onError = { err ->
                                                errorMessage = mapReportError(err)
                                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Send without proof", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Button
                val isSendingActive = submitState == ReportSubmitState.UPLOADING_PROOF ||
                        submitState == ReportSubmitState.SENDING_REPORT ||
                        submitState == ReportSubmitState.ATTACHING_PROOF

                Button(
                    onClick = {
                        if (reasonText.length > 1000) {
                            Toast.makeText(context, mapReportError("reason_too_long"), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        executeReportSubmit(
                            context = context,
                            scope = scope,
                            reportedUserId = reportedUserId,
                            reportedUserName = reportedUserName,
                            contextType = contextType,
                            contextId = contextId,
                            category = selectedReason,
                            reason = reasonText,
                            includeProof = includeProof,
                            dmProofMessages = dmProofMessages,
                            dmDeletionLog = dmDeletionLog,
                            imageUris = selectedImageUris,
                            existingScreenshotUrl = existingScreenshotUrl,
                            skipProofUpload = false,
                            onStateChange = { submitState = it },
                            onWarning = { attachmentWarning = it },
                            onError = { err ->
                                errorMessage = mapReportError(err)
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    enabled = !isSendingActive && submitState != ReportSubmitState.SUCCESS,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isSendingActive) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        val labelText = when (submitState) {
                            ReportSubmitState.UPLOADING_PROOF -> "Uploading proof..."
                            ReportSubmitState.ATTACHING_PROOF -> "Attaching proof..."
                            else -> "Sending..."
                        }
                        Text(labelText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
}

private fun executeReportSubmit(
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    reportedUserId: String,
    reportedUserName: String,
    contextType: String,
    contextId: String?,
    category: String,
    reason: String,
    includeProof: Boolean,
    dmProofMessages: List<Map<String, Any?>>?,
    dmDeletionLog: List<Map<String, Any?>>?,
    imageUris: List<Uri>,
    existingScreenshotUrl: String?,
    skipProofUpload: Boolean,
    onStateChange: (ReportSubmitState) -> Unit,
    onWarning: (String?) -> Unit,
    onError: (String) -> Unit
) {
    scope.launch {
        try {
            onWarning(null)
            val uploadedPaths = mutableListOf<String>()

            // 1. Upload proof screenshots if provided
            if (imageUris.isNotEmpty() && !skipProofUpload) {
                onStateChange(ReportSubmitState.UPLOADING_PROOF)
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                val selfUserId = SupabaseSessionManager.getUserId(context)

                if (token.isNullOrBlank() || selfUserId.isNullOrBlank()) {
                    onStateChange(ReportSubmitState.UPLOAD_FAILED)
                    onError("not_authenticated")
                    return@launch
                }

                val timestamp = System.currentTimeMillis()
                for (i in imageUris.indices) {
                    val uri = imageUris[i]
                    val bytes = compressAndShrinkImage(context, uri)
                    if (bytes == null) {
                        onStateChange(ReportSubmitState.UPLOAD_FAILED)
                        return@launch
                    }

                    val storagePath = "$selfUserId/$timestamp-$i.jpg"
                    val reqBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())

                    val uploadRes = SupabaseClient.api.uploadStorageRawBytes(
                        auth = token,
                        bucket = "report-proofs",
                        path = storagePath,
                        body = reqBody
                    )

                    if (uploadRes.isSuccessful) {
                        uploadedPaths.add(storagePath)
                    } else {
                        Log.e(TAG, "Screenshot upload failed for index $i: ${uploadRes.code()}")
                        onStateChange(ReportSubmitState.UPLOAD_FAILED)
                        return@launch
                    }
                }
            }

            // 2. Submit Main Report (report_user or report_group)
            onStateChange(ReportSubmitState.SENDING_REPORT)

            val evidenceMap: Map<String, Any?>? = if (includeProof && !dmProofMessages.isNullOrEmpty()) {
                mapOf("messages" to dmProofMessages)
            } else null

            val deletionLogList: List<Map<String, Any?>>? = if (includeProof && !dmDeletionLog.isNullOrEmpty()) {
                dmDeletionLog
            } else null

            val reportIdResult = if (contextType.lowercase() == "group") {
                submitGroupReportRpc(
                    context = context,
                    groupId = contextId ?: reportedUserId,
                    category = category,
                    reason = reason,
                    evidence = evidenceMap,
                    deletionLog = deletionLogList
                )
            } else {
                submitUserReportRpc(
                    context = context,
                    reportedUserId = reportedUserId,
                    contextType = contextType,
                    contextId = contextId,
                    category = category,
                    reason = reason,
                    evidence = evidenceMap,
                    deletionLog = deletionLogList,
                    existingScreenshotUrl = existingScreenshotUrl
                )
            }

            if (reportIdResult.isFailure) {
                val err = reportIdResult.exceptionOrNull()?.message ?: "Could not send, try again."
                onStateChange(ReportSubmitState.IDLE)
                onError(err)
                return@launch
            }

            val createdReportId = reportIdResult.getOrNull()

            // 3. Call attach_report_proofs if screenshot proof files were uploaded
            if (uploadedPaths.isNotEmpty() && !createdReportId.isNullOrBlank()) {
                onStateChange(ReportSubmitState.ATTACHING_PROOF)
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                if (token != null) {
                    val attachRes = SupabaseClient.api.attachReportProofs(
                        auth = token,
                        body = AttachReportProofsRequest(
                            p_report_id = createdReportId,
                            p_media_paths = uploadedPaths
                        )
                    )
                    if (!attachRes.isSuccessful) {
                        val rawErr = attachRes.errorBody()?.string().orEmpty()
                        Log.e(TAG, "attach_report_proofs failed: ${attachRes.code()} $rawErr")
                        val mapped = mapAttachError(rawErr)
                        onWarning("Your report was sent, but the screenshots could not be attached: $mapped")
                    } else {
                        Log.i(TAG, "attach_report_proofs succeeded for report $createdReportId")
                    }
                }
            }

            onStateChange(ReportSubmitState.SUCCESS)

        } catch (e: Exception) {
            Log.e(TAG, "Exception during report submit execution: ${e.message}", e)
            onStateChange(ReportSubmitState.IDLE)
            onError(e.localizedMessage ?: "Could not send, try again.")
        }
    }
}

private suspend fun submitUserReportRpc(
    context: Context,
    reportedUserId: String,
    contextType: String,
    contextId: String?,
    category: String,
    reason: String,
    evidence: Map<String, Any?>?,
    deletionLog: List<Map<String, Any?>>?,
    existingScreenshotUrl: String?
): Result<String?> = withContext(Dispatchers.IO) {
    try {
        val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
            ?: return@withContext Result.failure(Exception("not_authenticated"))

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

        val resp = SupabaseClient.api.reportUser(
            apiKey = BuildConfig.SUPABASE_ANON_KEY,
            auth = token,
            body = ReportUserRequest(
                p_reported_user_id = effectiveReportedUserId,
                p_context_type = contextType,
                p_category = category,
                p_reason = reason.ifBlank { null },
                p_context_id = contextId,
                p_screenshot_url = existingScreenshotUrl,
                p_evidence = evidence,
                p_deletion_log = deletionLog
            )
        )

        if (resp.isSuccessful) {
            val bodyStr = resp.body()?.string()
            val reportId = parseReportIdFromResponseBody(bodyStr)
            Result.success(reportId)
        } else {
            val code = resp.code()
            val rawErr = resp.errorBody()?.string().orEmpty()
            Result.failure(Exception("[$code] $rawErr"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private suspend fun submitGroupReportRpc(
    context: Context,
    groupId: String,
    category: String,
    reason: String,
    evidence: Map<String, Any?>?,
    deletionLog: List<Map<String, Any?>>?
): Result<String?> = withContext(Dispatchers.IO) {
    try {
        val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
            ?: return@withContext Result.failure(Exception("not_authenticated"))

        val resp = SupabaseClient.api.reportGroup(
            apiKey = BuildConfig.SUPABASE_ANON_KEY,
            auth = token,
            body = ReportGroupRequest(
                p_group_id = groupId.trim().replace("\"", ""),
                p_category = category,
                p_reason = reason.ifBlank { category },
                p_evidence = evidence,
                p_deletion_log = deletionLog
            )
        )

        if (resp.isSuccessful) {
            val bodyStr = resp.body()?.string()
            val reportId = parseReportIdFromResponseBody(bodyStr)
            Result.success(reportId)
        } else {
            val code = resp.code()
            val rawErr = resp.errorBody()?.string().orEmpty()
            Result.failure(Exception("[$code] $rawErr"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

fun compressAndShrinkImage(context: Context, uri: Uri): ByteArray? {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (originalBitmap == null) return null

        val width = originalBitmap.width
        val height = originalBitmap.height
        val maxDim = 1600

        val scaledBitmap = if (width > maxDim || height > maxDim) {
            val ratio = width.toFloat() / height.toFloat()
            val newWidth: Int
            val newHeight: Int
            if (width >= height) {
                newWidth = maxDim
                newHeight = (maxDim / ratio).toInt()
            } else {
                newHeight = maxDim
                newWidth = (maxDim * ratio).toInt()
            }
            Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
        } else {
            originalBitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val bytes = outputStream.toByteArray()
        if (scaledBitmap != originalBitmap) {
            scaledBitmap.recycle()
        }
        originalBitmap.recycle()
        return bytes
    } catch (e: Exception) {
        Log.e(TAG, "Error compressing image: ${e.message}", e)
        return null
    }
}

fun parseReportIdFromResponseBody(bodyStr: String?): String? {
    if (bodyStr.isNullOrBlank()) return null
    return try {
        val trimmed = bodyStr.trim()
        if (trimmed.startsWith("{")) {
            val json = JSONObject(trimmed)
            json.optString("id").takeIf { it.isNotBlank() }
                ?: json.optString("report_id").takeIf { it.isNotBlank() }
                ?: json.optString("p_report_id").takeIf { it.isNotBlank() }
        } else if (trimmed.startsWith("[")) {
            val arr = JSONArray(trimmed)
            if (arr.length() > 0) {
                val item = arr.getJSONObject(0)
                item.optString("id").takeIf { it.isNotBlank() }
                    ?: item.optString("report_id").takeIf { it.isNotBlank() }
            } else null
        } else if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            trimmed.substring(1, trimmed.length - 1)
        } else if (SupabaseSessionManager.isValidUuid(trimmed)) {
            trimmed
        } else null
    } catch (_: Exception) {
        null
    }
}

fun mapAttachError(raw: String?): String {
    if (raw.isNullOrBlank()) return "Failed to attach screenshot proofs."
    val lower = raw.lowercase()
    return when {
        lower.contains("not_your_report") -> "Report does not belong to you."
        lower.contains("attach_window_closed") -> "Attachment window has expired (30 minutes)."
        lower.contains("file_not_found") -> "Screenshot proof file not found in storage."
        lower.contains("invalid_path") -> "Invalid storage path for proof."
        lower.contains("too_many_files") -> "Maximum proof files limit exceeded."
        else -> raw.trim()
    }
}

fun mapReportError(raw: String?): String {
    if (raw.isNullOrBlank()) return "Could not send, try again."
    val lower = raw.lowercase()

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
