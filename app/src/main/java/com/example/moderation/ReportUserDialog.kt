package com.example.moderation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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
import com.example.ui.components.SoftCloseIcon
import com.example.ui.components.SoftShieldIcon
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportUserDialog(
    reportedUserId: String,
    reportedUserName: String,
    contextType: String, // "profile", "dm", "moment"
    contextId: String? = null,
    dmMessageText: String? = null,
    existingScreenshotUrl: String? = null,
    onDismiss: () -> Unit,
    onReportSubmitted: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val categories = remember {
        listOf(
            "Harassment & Bullying",
            "Inappropriate Content / NSFW",
            "Spam & Scam",
            "Hate Speech",
            "Impersonation",
            "Other"
        )
    }

    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var reasonText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

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
            Text(
                text = "Additional Details (Optional)",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF334155)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(12.dp))
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
                    onValueChange = { reasonText = it },
                    textStyle = TextStyle(color = Color.Black, fontSize = 13.sp),
                    cursorBrush = SolidColor(Color.Black),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (contextType == "dm") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "📸 A client-side snapshot of the decrypted message will be attached as evidence so moderators can review it.",
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Submit Button
            Button(
                onClick = {
                    isSubmitting = true
                    scope.launch {
                        submitReport(
                            context = context,
                            reportedUserId = reportedUserId,
                            reportedUserName = reportedUserName,
                            contextType = contextType,
                            contextId = contextId,
                            category = selectedCategory,
                            reason = reasonText.ifBlank { selectedCategory },
                            dmMessageText = dmMessageText,
                            existingScreenshotUrl = existingScreenshotUrl,
                            onSuccess = {
                                isSubmitting = false
                                Toast.makeText(context, "Report submitted. Thank you for keeping FunkyTalk safe!", Toast.LENGTH_LONG).show()
                                onReportSubmitted()
                                onDismiss()
                            },
                            onError = { err ->
                                isSubmitting = false
                                Toast.makeText(context, "Failed to submit report: $err", Toast.LENGTH_SHORT).show()
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
    reportedUserName: String,
    contextType: String,
    contextId: String?,
    category: String,
    reason: String,
    dmMessageText: String?,
    existingScreenshotUrl: String?,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
            ?: return@withContext onError("Please log in to submit a report")

        var screenshotUrl = existingScreenshotUrl

        // For DM reports: render the decrypted message into a bitmap snapshot and upload to report-evidence
        if (contextType == "dm" && screenshotUrl.isNullOrBlank() && !dmMessageText.isNullOrBlank()) {
            try {
                val bitmap = renderMessageToBitmap(reportedUserName, dmMessageText)
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                val bytes = stream.toByteArray()
                val filename = "dm_report_${UUID.randomUUID()}.jpg"

                val reqBody = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())

                // Upload to report-evidence bucket (or fallback to reports/avatars)
                val uploadRes = SupabaseClient.api.uploadStorageRawBytes(
                    auth = token,
                    bucket = "report-evidence",
                    filename = filename,
                    contentType = "image/jpeg",
                    body = reqBody
                )

                if (uploadRes.isSuccessful) {
                    val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
                    val host = if (baseUrl.startsWith("http")) baseUrl else "https://$baseUrl"
                    screenshotUrl = "$host/storage/v1/object/public/report-evidence/$filename"
                } else {
                    // Fallback to avatars bucket if report-evidence doesn't allow raw write
                    val fbRes = SupabaseClient.api.uploadStorageRawBytes(
                        auth = token,
                        bucket = "avatars",
                        filename = filename,
                        contentType = "image/jpeg",
                        body = reqBody
                    )
                    if (fbRes.isSuccessful) {
                        val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
                        val host = if (baseUrl.startsWith("http")) baseUrl else "https://$baseUrl"
                        screenshotUrl = "$host/storage/v1/object/public/avatars/$filename"
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("ReportUser", "Screenshot capture/upload warning: ${e.message}")
            }
        }

        val resp = SupabaseClient.api.reportUser(
            apiKey = BuildConfig.SUPABASE_ANON_KEY,
            auth = token,
            body = ReportUserRequest(
                p_reported_user_id = reportedUserId,
                p_context_type = contextType,
                p_category = category,
                p_reason = reason,
                p_context_id = contextId,
                p_screenshot_url = screenshotUrl
            )
        )

        if (resp.isSuccessful) {
            withContext(Dispatchers.Main) { onSuccess() }
        } else {
            val err = resp.errorBody()?.string() ?: "Failed to report user"
            withContext(Dispatchers.Main) { onError(err) }
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) { onError(e.localizedMessage ?: "Unknown error") }
    }
}

private fun renderMessageToBitmap(
    senderName: String,
    messageText: String
): Bitmap {
    val width = 720
    val height = 360
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = Paint().apply { isAntiAlias = true }

    // Card background
    paint.color = android.graphics.Color.parseColor("#FEF3C7")
    canvas.drawRoundRect(30f, 30f, 690f, 330f, 24f, 24f, paint)

    // Sender title
    paint.color = android.graphics.Color.parseColor("#92400E")
    paint.textSize = 28f
    paint.isFakeBoldText = true
    canvas.drawText("Encrypted DM Evidence: $senderName", 60f, 80f, paint)

    // Decrypted Plaintext
    paint.color = android.graphics.Color.parseColor("#0F172A")
    paint.textSize = 28f
    paint.isFakeBoldText = false
    val lines = messageText.chunked(36)
    var y = 140f
    for (line in lines.take(4)) {
        canvas.drawText(line, 60f, y, paint)
        y += 40f
    }

    return bitmap
}
