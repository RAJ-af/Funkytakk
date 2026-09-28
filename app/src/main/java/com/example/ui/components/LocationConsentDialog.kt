package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun LocationConsentDialog(
    onDismiss: () -> Unit,
    onConfirmConsent: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
                .border(1.dp, Color(0xFFECEBE6), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Circular Soft Icon Shield
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEF3C7)),
                    contentAlignment = Alignment.Center
                ) {
                    ConsentMarkerIcon(size = 28.dp, color = Color(0xFFD97706))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = "Location Consent & Terms",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle / Terms description
                Text(
                    text = "To auto-detect your current city, recommend local language rooms, and tag posts with your location, we need your consent to access device location.",
                    fontSize = 13.sp,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Feature Highlights Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF9F6)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ConsentBulletItem(text = "Auto-fill city & country in profile")
                        ConsentBulletItem(text = "Tag posts with your current location")
                        ConsentBulletItem(text = "Find language exchange partners nearby")
                        ConsentBulletItem(text = "Your exact location is private & encrypted")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel / Disagree Button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF64748B))
                    ) {
                        Text("Not Now", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                    }

                    // Agree & Allow Button
                    Button(
                        onClick = onConfirmConsent,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD43B), contentColor = Color(0xFF0F172A))
                    ) {
                        Text("Agree & Allow", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsentBulletItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Color(0xFFD1FAE5)),
            contentAlignment = Alignment.Center
        ) {
            ConsentCheckIcon(size = 9.dp, color = Color(0xFF059669))
        }
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color(0xFF334155),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ConsentMarkerIcon(size: androidx.compose.ui.unit.Dp, color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val center = androidx.compose.ui.geometry.Offset(w / 2f, h * 0.38f)
        val radius = w * 0.34f
        drawCircle(color = color, radius = radius, center = center)
        drawCircle(color = Color.White, radius = radius * 0.45f, center = center)
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(center.x - radius * 0.85f, center.y + radius * 0.35f)
            lineTo(center.x, h * 0.95f)
            lineTo(center.x + radius * 0.85f, center.y + radius * 0.35f)
            close()
        }
        drawPath(path = path, color = color)
    }
}

@Composable
private fun ConsentCheckIcon(size: androidx.compose.ui.unit.Dp, color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.15f, h * 0.5f)
            lineTo(w * 0.42f, h * 0.82f)
            lineTo(w * 0.88f, h * 0.2f)
        }
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}
