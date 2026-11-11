package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MyModerationStatus
import com.example.R
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SuspendedAccountScreen(
    moderationStatus: MyModerationStatus?,
    onStatusRefreshed: () -> Unit = {},
    onAppeal: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Prevent going back when suspended
    BackHandler(enabled = true) {
        // Back press is strictly disabled during suspension
    }

    // Auto-poll status periodically to restore immediately if unbanned/revoked
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            onStatusRefreshed()
        }
    }

    // Infinite transitions for dynamic non-circular organic background shapes with variable speed rotation (slow to fast easing)
    val infiniteTransition = rememberInfiniteTransition(label = "organic_blobs_motion")

    // Primary blob: 360 degree rotation with EaseInOutInterpolator for slow-fast dynamic speed
    val blobRotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 14000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "blob_rotate_primary"
    )

    // Secondary blob: counter-rotation with pulsating slow-fast motion
    val secondaryRotationAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 20000,
                easing = CubicBezierEasing(0.42f, 0.0f, 0.58f, 1.0f)
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "blob_rotate_secondary"
    )

    // Organic morphing / breathing scale effect (slow expansion and gentle compression)
    val blobMorphScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 5000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blob_morph_scale"
    )

    // Gentle floating motion for the lock icon
    val lockFloatingOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2600,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lock_float"
    )

    // Format suspension time
    val formattedExpiry = remember(moderationStatus?.expires_at) {
        formatSuspensionExpiry(moderationStatus?.expires_at)
    }

    val displayReason = remember(moderationStatus?.reason) {
        val r = moderationStatus?.reason?.trim()
        if (r.isNullOrBlank()) "Community Guidelines violation" else r
    }

    val brandYellow = Color(0xFFFBBF24)
    val brandYellowText = Color(0xFFEAB308)
    val cardBgColor = Color(0xFFFAF7EE)
    val textPrimary = Color(0xFF111827)
    val textSecondary = Color(0xFF6B7280)
    val textMuted = Color(0xFF9CA3AF)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDF2))
    ) {
        // Dynamic non-circular organic custom shapes (morphing curved blobs, not circles) with slow-fast rotation
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // 1. Top-right dynamic amoeba / fluid organic blob
            val topCenterX = width * 0.88f
            val topCenterY = height * 0.14f

            rotate(degrees = blobRotationAngle, pivot = Offset(topCenterX, topCenterY)) {
                val blobPathTop = createAmoebaBlobPath(
                    centerX = topCenterX,
                    centerY = topCenterY,
                    radius = (width * 0.42f) * blobMorphScale,
                    wobbleFactor = 0.35f
                )
                drawPath(
                    path = blobPathTop,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFDE68A).copy(alpha = 0.65f),
                            Color(0xFFFEF08A).copy(alpha = 0.35f),
                            Color(0xFFFFFBEB).copy(alpha = 0.0f)
                        ),
                        center = Offset(topCenterX, topCenterY),
                        radius = width * 0.45f
                    )
                )
            }

            // 2. Middle-center halo organic asymmetric fluid shape behind the 3D lock
            val midCenterX = width * 0.5f
            val midCenterY = height * 0.33f

            rotate(degrees = secondaryRotationAngle, pivot = Offset(midCenterX, midCenterY)) {
                val midAmoebaPath = createMultiLobedBlobPath(
                    centerX = midCenterX,
                    centerY = midCenterY,
                    baseRadius = (width * 0.46f) * blobMorphScale,
                    lobes = 5
                )
                drawPath(
                    path = midAmoebaPath,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFDE047).copy(alpha = 0.45f),
                            Color(0xFFFEF3C7).copy(alpha = 0.28f),
                            Color(0xFFFFFDF2).copy(alpha = 0.0f)
                        ),
                        center = Offset(midCenterX, midCenterY),
                        radius = width * 0.52f
                    )
                )
            }

            // 3. Bottom-left organic non-circular decorative blob
            val bottomCenterX = width * 0.10f
            val bottomCenterY = height * 0.90f

            rotate(degrees = blobRotationAngle * 0.7f, pivot = Offset(bottomCenterX, bottomCenterY)) {
                val bottomBlobPath = createAmoebaBlobPath(
                    centerX = bottomCenterX,
                    centerY = bottomCenterY,
                    radius = (width * 0.45f) * (2f - blobMorphScale),
                    wobbleFactor = 0.4f
                )
                drawPath(
                    path = bottomBlobPath,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFDE68A).copy(alpha = 0.55f),
                            Color(0xFFFEF08A).copy(alpha = 0.25f),
                            Color(0xFFFFFDF2).copy(alpha = 0.0f)
                        ),
                        center = Offset(bottomCenterX, bottomCenterY),
                        radius = width * 0.48f
                    )
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // FunkyTalk Brand Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Funky",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Talk",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Black,
                        color = brandYellowText,
                        letterSpacing = (-0.5).sp
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Lock Asset from User's Google Drive link with floating translation
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .offset(y = lockFloatingOffset.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_account_suspended_lock),
                        contentDescription = "Account Suspended Lock",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Main Info Card (Clean White Rounded Container)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(32.dp),
                            spotColor = Color(0x18000000),
                            ambientColor = Color(0x0A000000)
                        ),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title: Account Suspended
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    style = SpanStyle(
                                        color = textPrimary,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 24.sp
                                    )
                                ) {
                                    append("Account ")
                                }
                                withStyle(
                                    style = SpanStyle(
                                        color = brandYellowText,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 24.sp
                                    )
                                ) {
                                    append("Suspended")
                                }
                            },
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Subtitle
                        Text(
                            text = "Your account has been temporarily\nsuspended.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = textSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Reason Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(cardBgColor)
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Reason",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textMuted
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = displayReason,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Suspended Until Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(cardBgColor)
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Suspended until",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textMuted
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formattedExpiry,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Action Area: Appeal button & Exit App Pill Button
            val supportSession by com.example.SupportChatManager.session.collectAsState()
            val canAppeal = supportSession?.can_appeal == true || supportSession?.open == true || supportSession?.reason == "appeal"

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (canAppeal) {
                    Button(
                        onClick = onAppeal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(100.dp),
                                spotColor = Color(0x3DFBBF24)
                            ),
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = brandYellow,
                            contentColor = Color.Black
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = "Appeal this decision",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = {
                            activity?.finishAffinity()
                        }
                    ) {
                        Text(
                            text = "Exit App",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textSecondary
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            activity?.finishAffinity()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(100.dp),
                                spotColor = Color(0x3DFBBF24)
                            ),
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = brandYellow,
                            contentColor = Color.Black
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = "Exit App",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

/**
 * Creates an organic asymmetrical amoeba shape using Bezier control curves (strictly non-circular).
 */
private fun createAmoebaBlobPath(
    centerX: Float,
    centerY: Float,
    radius: Float,
    wobbleFactor: Float = 0.3f
): Path {
    val path = Path()
    val numPoints = 6
    val angleStep = (2 * Math.PI / numPoints).toFloat()
    val radii = floatArrayOf(
        radius * (1f + wobbleFactor * 0.8f),
        radius * (1f - wobbleFactor * 0.9f),
        radius * (1f + wobbleFactor * 1.1f),
        radius * (1f - wobbleFactor * 0.6f),
        radius * (1f + wobbleFactor * 0.9f),
        radius * (1f - wobbleFactor * 1.0f)
    )

    val points = mutableListOf<Offset>()
    for (i in 0 until numPoints) {
        val angle = i * angleStep
        val r = radii[i % radii.size]
        val x = centerX + r * cos(angle.toDouble()).toFloat()
        val y = centerY + r * sin(angle.toDouble()).toFloat()
        points.add(Offset(x, y))
    }

    if (points.isNotEmpty()) {
        path.moveTo(
            (points[0].x + points[numPoints - 1].x) / 2f,
            (points[0].y + points[numPoints - 1].y) / 2f
        )
        for (i in 0 until numPoints) {
            val curr = points[i]
            val next = points[(i + 1) % numPoints]
            val midX = (curr.x + next.x) / 2f
            val midY = (curr.y + next.y) / 2f
            path.quadraticTo(curr.x, curr.y, midX, midY)
        }
        path.close()
    }
    return path
}

/**
 * Creates an asymmetrical multi-lobed fluid organic shape with smooth quadratic spline curves.
 */
private fun createMultiLobedBlobPath(
    centerX: Float,
    centerY: Float,
    baseRadius: Float,
    lobes: Int = 5
): Path {
    val path = Path()
    val steps = 18
    val angleStep = (2 * Math.PI / steps).toFloat()
    val points = mutableListOf<Offset>()

    for (i in 0 until steps) {
        val angle = i * angleStep
        val variation = 0.22f * sin((lobes * angle).toDouble()).toFloat() + 0.12f * cos((2 * angle).toDouble()).toFloat()
        val r = baseRadius * (1f + variation)
        val x = centerX + r * cos(angle.toDouble()).toFloat()
        val y = centerY + r * sin(angle.toDouble()).toFloat()
        points.add(Offset(x, y))
    }

    if (points.isNotEmpty()) {
        val last = points.last()
        path.moveTo((points[0].x + last.x) / 2f, (points[0].y + last.y) / 2f)
        for (i in points.indices) {
            val curr = points[i]
            val next = points[(i + 1) % points.size]
            val midX = (curr.x + next.x) / 2f
            val midY = (curr.y + next.y) / 2f
            path.quadraticTo(curr.x, curr.y, midX, midY)
        }
        path.close()
    }
    return path
}

private fun formatSuspensionExpiry(rawExpiresAt: String?): String {
    if (rawExpiresAt.isNullOrBlank()) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val outSdf = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())
        return outSdf.format(cal.time)
    }

    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss"
    )

    for (pattern in formats) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(rawExpiresAt)
            if (date != null) {
                val outSdf = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())
                return outSdf.format(date)
            }
        } catch (_: Exception) {
            // Try next format
        }
    }

    return rawExpiresAt
}
