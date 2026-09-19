package com.example.ui.screens.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.UserProfile

@Composable
fun HomeGreetingCard(
    currentUserProfile: UserProfile,
    greetingText: String = "Good Morning"
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT: Circular outlined sun icon in yellow
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                    // Draw outer circle dashed/outlined
                    drawCircle(
                        color = Color(0xFFFDE68A),
                        radius = size.width / 2 - 2.dp.toPx(),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                        )
                    )
                    // Draw center sun circle
                    drawCircle(
                        color = Color(0xFFFFB300),
                        radius = 12.dp.toPx()
                    )
                    // Draw 8 rays
                    val numRays = 8
                    val rayLength = 6.dp.toPx()
                    val rayStart = 15.dp.toPx()
                    for (i in 0 until numRays) {
                        val angle = (i * 360f / numRays) * (Math.PI / 180f)
                        val startX = center.x + rayStart * Math.cos(angle).toFloat()
                        val startY = center.y + rayStart * Math.sin(angle).toFloat()
                        val endX = center.x + (rayStart + rayLength) * Math.cos(angle).toFloat()
                        val endY = center.y + (rayStart + rayLength) * Math.sin(angle).toFloat()
                        drawLine(
                            color = Color(0xFFFFB300),
                            start = androidx.compose.ui.geometry.Offset(startX, startY),
                            end = androidx.compose.ui.geometry.Offset(endX, endY),
                            strokeWidth = 2.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // CENTER: Text Section
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                val displayGreetName = (currentUserProfile.displayName?.ifBlank { "Batto" } 
                    ?: currentUserProfile.username?.ifBlank { "Batto" } 
                    ?: "Batto")
                
                // Text 1 (Bold 30sp): Good Morning, {UserName}!
                Text(
                    text = "$greetingText, $displayGreetName!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    color = Color(0xFF453000),
                    lineHeight = 34.sp
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                // Text 2 (18sp) & Text 3 with fluency. ✨ highlighted
                val annotatedText = buildAnnotatedString {
                    append("Every conversation you have today\nbrings you one step closer to ")
                    withStyle(style = SpanStyle(color = Color(0xFFF6C945), fontWeight = FontWeight.Bold)) {
                        append("fluency.")
                    }
                    append(" ✨")
                }
                Text(
                    text = annotatedText,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                    color = Color(0xFF4B5563)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // RIGHT: Yellow paper plane, flight path, hearts, faded globe
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val centerX = w * 0.7f
                    val centerY = h * 0.5f
                    val radius = h * 0.45f
                    
                    // Draw the globe outline (faded yellow)
                    drawCircle(
                        color = Color(0xFFFFB300).copy(alpha = 0.08f),
                        radius = radius,
                        center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                    )
                    // Draw grid lines on the globe
                    drawArc(
                        color = Color(0xFFFFB300).copy(alpha = 0.06f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(centerX - radius, centerY - radius * 0.5f),
                        size = androidx.compose.ui.geometry.Size(radius * 2f, radius),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawArc(
                        color = Color(0xFFFFB300).copy(alpha = 0.06f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(centerX - radius * 0.5f, centerY - radius),
                        size = androidx.compose.ui.geometry.Size(radius, radius * 2f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    
                    // Flight path (S-curve dashed line)
                    val path = Path().apply {
                        moveTo(w * 0.1f, h * 0.8f)
                        cubicTo(
                            w * 0.4f, h * 1.1f,
                            w * 0.1f, h * 0.2f,
                            w * 0.9f, h * 0.4f
                        )
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFF6B7280).copy(alpha = 0.4f),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    )
                }

                // Hearts positioned around path
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.CenterStart)
                        .offset(x = 10.dp, y = (-15).dp)
                )
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.BottomCenter)
                        .offset(x = (-10).dp, y = (-20).dp)
                )
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier
                        .size(6.dp)
                        .align(Alignment.TopCenter)
                        .offset(x = 15.dp, y = 15.dp)
                )

                // Yellow paper plane (Send icon rotated)
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Paper plane",
                    tint = Color(0xFFFFB300),
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 5.dp, y = 20.dp)
                        .rotate(-35f)
                )
            }
        }
    }
}
