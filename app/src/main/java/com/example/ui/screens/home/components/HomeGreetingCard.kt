package com.example.ui.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.UserProfile
import com.example.ui.components.FunkySendIcon
import com.example.ui.theme.LocalAppDarkTheme
import com.example.ui.theme.LocalFunkyColors
import java.util.Calendar

private enum class TimeOfDay {
    MORNING, AFTERNOON, EVENING, NIGHT
}

private data class GreetingConfig(
    val greeting: String,
    val subtitle: String,
    val accentColor: Color,
    val containerBg: Color,
    val borderColor: Color,
    val timeOfDay: TimeOfDay
)

@Composable
fun HomeGreetingCard(
    currentUserProfile: UserProfile,
    greetingText: String? = null
) {
    val calendar = remember { Calendar.getInstance() }
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val isDark = LocalAppDarkTheme.current
    val funkyColors = LocalFunkyColors.current

    val config = remember(hour, isDark, funkyColors) {
        when {
            hour in 5..11 -> GreetingConfig(
                greeting = "Good Morning",
                subtitle = "Start your day with a fun conversation",
                accentColor = if (isDark) funkyColors.mango else Color(0xFFF59E0B),
                containerBg = if (isDark) funkyColors.surface else Color(0xFFFFFBEB),
                borderColor = if (isDark) funkyColors.line else Color(0xFFFDE68A).copy(alpha = 0.7f),
                timeOfDay = TimeOfDay.MORNING
            )
            hour in 12..16 -> GreetingConfig(
                greeting = "Good Afternoon",
                subtitle = "Take a break and practice with friends",
                accentColor = if (isDark) funkyColors.mango else Color(0xFFD97706),
                containerBg = if (isDark) funkyColors.surface else Color(0xFFFFFDF5),
                borderColor = if (isDark) funkyColors.line else Color(0xFFFCD34D).copy(alpha = 0.6f),
                timeOfDay = TimeOfDay.AFTERNOON
            )
            hour in 17..20 -> GreetingConfig(
                greeting = "Good Evening",
                subtitle = "Unwind and connect in lively voice circles",
                accentColor = if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C),
                containerBg = if (isDark) funkyColors.surface else Color(0xFFFFF7ED),
                borderColor = if (isDark) funkyColors.line else Color(0xFFFED7AA).copy(alpha = 0.7f),
                timeOfDay = TimeOfDay.EVENING
            )
            else -> GreetingConfig(
                greeting = "Good Night",
                subtitle = "Night owls are practicing speaking right now",
                accentColor = if (isDark) Color(0xFFA5B4FC) else Color(0xFF6366F1),
                containerBg = if (isDark) funkyColors.surface else Color(0xFFF6F6FD),
                borderColor = if (isDark) funkyColors.line else Color(0xFFE0E7FF).copy(alpha = 0.8f),
                timeOfDay = TimeOfDay.NIGHT
            )
        }
    }

    val finalGreeting = if (!greetingText.isNullOrBlank() && greetingText != "Good Morning") {
        greetingText
    } else {
        config.greeting
    }

    val displayGreetName = (currentUserProfile.displayName?.ifBlank { null }
        ?: currentUserProfile.username?.ifBlank { null }
        ?: "Friend")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = config.containerBg),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, config.borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Minimalist Time-of-day Icon Container
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(config.accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(28.dp)) {
                    val w = size.width
                    val h = size.height
                    val center = Offset(w / 2, h / 2)

                    when (config.timeOfDay) {
                        TimeOfDay.MORNING, TimeOfDay.AFTERNOON -> {
                            val sunRadius = w * 0.26f
                            drawCircle(
                                color = config.accentColor,
                                radius = sunRadius,
                                center = center
                            )
                            val rayCount = 8
                            val innerRay = sunRadius + 2.5.dp.toPx()
                            val outerRay = sunRadius + 5.5.dp.toPx()
                            for (i in 0 until rayCount) {
                                val angle = (i * 360f / rayCount) * (Math.PI / 180f)
                                val sx = center.x + innerRay * Math.cos(angle).toFloat()
                                val sy = center.y + innerRay * Math.sin(angle).toFloat()
                                val ex = center.x + outerRay * Math.cos(angle).toFloat()
                                val ey = center.y + outerRay * Math.sin(angle).toFloat()
                                drawLine(
                                    color = config.accentColor,
                                    start = Offset(sx, sy),
                                    end = Offset(ex, ey),
                                    strokeWidth = 1.8.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                        TimeOfDay.EVENING -> {
                            val sunRadius = w * 0.28f
                            drawArc(
                                color = config.accentColor,
                                startAngle = 180f,
                                sweepAngle = 180f,
                                useCenter = true,
                                topLeft = Offset(center.x - sunRadius, center.y - sunRadius),
                                size = Size(sunRadius * 2, sunRadius * 2)
                            )
                            drawLine(
                                color = config.accentColor,
                                start = Offset(w * 0.12f, center.y + 1.dp.toPx()),
                                end = Offset(w * 0.88f, center.y + 1.dp.toPx()),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = config.accentColor.copy(alpha = 0.5f),
                                start = Offset(w * 0.25f, center.y + 5.dp.toPx()),
                                end = Offset(w * 0.75f, center.y + 5.dp.toPx()),
                                strokeWidth = 1.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                        TimeOfDay.NIGHT -> {
                            val moonPath = Path().apply {
                                moveTo(w * 0.65f, h * 0.15f)
                                cubicTo(
                                    w * 0.25f, h * 0.25f,
                                    w * 0.25f, h * 0.75f,
                                    w * 0.65f, h * 0.85f
                                )
                                cubicTo(
                                    w * 0.40f, h * 0.70f,
                                    w * 0.40f, h * 0.30f,
                                    w * 0.65f, h * 0.15f
                                )
                                close()
                            }
                            drawPath(
                                path = moonPath,
                                color = config.accentColor
                            )
                            val starX = w * 0.78f
                            val starY = h * 0.32f
                            drawLine(
                                color = config.accentColor,
                                start = Offset(starX - 2.5.dp.toPx(), starY),
                                end = Offset(starX + 2.5.dp.toPx(), starY),
                                strokeWidth = 1.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = config.accentColor,
                                start = Offset(starX, starY - 2.5.dp.toPx()),
                                end = Offset(starX, starY + 2.5.dp.toPx()),
                                strokeWidth = 1.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = finalGreeting,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = funkyColors.ink
                    )
                    Text(
                        text = ", $displayGreetName",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = config.accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = config.subtitle,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = funkyColors.mute
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(config.accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                FunkySendIcon(
                    color = config.accentColor,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(-35f)
                )
            }
        }
    }
}
