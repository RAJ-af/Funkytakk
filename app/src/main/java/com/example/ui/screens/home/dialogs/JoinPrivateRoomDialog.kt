package com.example.ui.screens.home.dialogs

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.components.SoftBackIcon
import com.example.ui.components.SoftShieldIcon
import com.example.ui.theme.LocalFunkyColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.GoldAccent

@Composable
fun JoinPrivateRoomDialog(
    onDismiss: () -> Unit,
    onJoin: (code: String) -> Unit,
    isLoading: Boolean = false
) {
    val context = LocalContext.current
    val funkyColors = LocalFunkyColors.current
    val isDark = funkyColors.isDark
    var code by remember { mutableStateOf("") }

    // Auto submit when 6 digits are reached
    LaunchedEffect(code) {
        if (code.length == 6 && !isLoading) {
            onJoin(code)
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isLoading) onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isDark) 0.72f else 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (!isLoading) onDismiss()
                },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {} // Prevent click-through
                    .wrapContentHeight(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = funkyColors.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                border = BorderStroke(1.5.dp, if (isDark) funkyColors.mango.copy(alpha = 0.45f) else Color(0xFFFDE68A))
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Decorative ambient glow backdrop in top corners
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        funkyColors.mango.copy(alpha = if (isDark) 0.14f else 0.22f),
                                        funkyColors.mango.copy(alpha = if (isDark) 0.04f else 0.08f),
                                        funkyColors.surface
                                    )
                                )
                            )
                    )

                    // Top close button
                    IconButton(
                        onClick = { if (!isLoading) onDismiss() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(36.dp)
                            .shadow(2.dp, CircleShape)
                            .background(funkyColors.soft, CircleShape)
                            .border(1.dp, funkyColors.line, CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_close),
                            contentDescription = "Close",
                            tint = funkyColors.ink,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Premium Lock Badge with Double Gradient Rings
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .shadow(8.dp, CircleShape, spotColor = Color(0xFFF59E0B).copy(alpha = 0.4f))
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0xFFFFF7ED), Color(0xFFFDE68A), Color(0xFFFFB800))
                                    ),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 3.dp,
                                    brush = Brush.sweepGradient(
                                        listOf(Color(0xFFFFD54F), Color(0xFFF59E0B), Color(0xFFFFD54F))
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_lock),
                                contentDescription = null,
                                tint = Color(0xFF92400E),
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Category Pill Tag
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = if (isDark) funkyColors.mango.copy(alpha = 0.16f) else Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, if (isDark) funkyColors.mango.copy(alpha = 0.45f) else Color(0xFFFCD34D))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SoftShieldIcon(
                                    size = 13.dp,
                                    color = if (isDark) funkyColors.mango else Color(0xFFB45309)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "PRIVATE VOICE ROOM",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) funkyColors.mango else Color(0xFFB45309),
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dialog Title
                        Text(
                            text = "Enter Room Code",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = funkyColors.ink,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Subtitle
                        Text(
                            text = "Enter the 6-digit passcode shared by the host to connect immediately.",
                            fontSize = 13.sp,
                            color = funkyColors.mute,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // 6-Digit Visual Code Boxes
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {

                            // 6 Distinct OTP Digit Tiles
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 0 until 6) {
                                    val char = code.getOrNull(i)?.toString() ?: ""
                                    val isCurrent = i == code.length && code.length < 6
                                    val isFilled = char.isNotEmpty()

                                    val backgroundColor = when {
                                        isFilled -> if (isDark) funkyColors.mango.copy(alpha = 0.16f) else Color(0xFFFFFDF5)
                                        isCurrent -> if (isDark) funkyColors.mango.copy(alpha = 0.24f) else Color(0xFFFFFBEB)
                                        else -> funkyColors.soft
                                    }

                                    val borderColor = when {
                                        isCurrent -> funkyColors.mango
                                        isFilled -> funkyColors.mango.copy(alpha = 0.7f)
                                        else -> funkyColors.line
                                    }

                                    val borderWidth = if (isCurrent || isFilled) 2.dp else 1.2.dp

                                    Box(
                                        modifier = Modifier
                                            .size(width = 44.dp, height = 54.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(backgroundColor)
                                            .border(borderWidth, borderColor, RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (char.isNotEmpty()) {
                                            Text(
                                                text = char,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Black,
                                                color = funkyColors.ink,
                                                textAlign = TextAlign.Center
                                            )
                                        } else if (isCurrent) {
                                            // Blinking cursor
                                            val infiniteTransition = rememberInfiniteTransition(label = "cursor")
                                            val alpha by infiniteTransition.animateFloat(
                                                initialValue = 0.2f,
                                                targetValue = 1f,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(500, easing = LinearEasing),
                                                    repeatMode = RepeatMode.Reverse
                                                ),
                                                label = "alpha"
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 2.dp, height = 22.dp)
                                                    .background(funkyColors.mango.copy(alpha = alpha), RoundedCornerShape(1.dp))
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .background(funkyColors.mute.copy(alpha = 0.45f), CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Actions: Paste from Clipboard & Clear
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = {
                                    try {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = clipboard.primaryClip
                                        if (clip != null && clip.itemCount > 0) {
                                            val pasteText = clip.getItemAt(0).text?.toString() ?: ""
                                            val digits = pasteText.filter { it.isDigit() }.take(6)
                                            if (digits.isNotEmpty()) {
                                                code = digits
                                                Toast.makeText(context, "Pasted passcode: $digits", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "No numeric passcode found in clipboard", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not read clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(100.dp),
                                color = funkyColors.soft,
                                border = BorderStroke(1.dp, funkyColors.line),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_copy),
                                        contentDescription = null,
                                        tint = funkyColors.ink,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Paste Code",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = funkyColors.ink
                                    )
                                }
                            }

                            if (code.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Surface(
                                    onClick = { code = "" },
                                    shape = RoundedCornerShape(100.dp),
                                    color = if (isDark) funkyColors.danger.copy(alpha = 0.16f) else Color(0xFFFEE2E2),
                                    border = BorderStroke(1.dp, if (isDark) funkyColors.danger.copy(alpha = 0.45f) else Color(0xFFFECACA)),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = "Clear",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = funkyColors.danger,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Built-in Numeric Keypad for fast and smooth mobile input
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val keys = listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5", "6"),
                                listOf("7", "8", "9"),
                                listOf("paste", "0", "back")
                            )

                            for (row in keys) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    for (key in row) {
                                        when (key) {
                                            "back" -> {
                                                Surface(
                                                    onClick = {
                                                        if (code.isNotEmpty()) {
                                                            code = code.dropLast(1)
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = funkyColors.soft,
                                                    border = BorderStroke(1.dp, funkyColors.line),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(44.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        SoftBackIcon(
                                                            size = 18.dp,
                                                            color = funkyColors.ink
                                                        )
                                                    }
                                                }
                                            }
                                            "paste" -> {
                                                Surface(
                                                    onClick = {
                                                        try {
                                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                            val clip = clipboard.primaryClip
                                                            if (clip != null && clip.itemCount > 0) {
                                                                val pasteText = clip.getItemAt(0).text?.toString() ?: ""
                                                                val digits = pasteText.filter { it.isDigit() }.take(6)
                                                                if (digits.isNotEmpty()) {
                                                                    code = digits
                                                                }
                                                            }
                                                        } catch (_: Exception) {}
                                                    },
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = funkyColors.soft,
                                                    border = BorderStroke(1.dp, funkyColors.line),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(44.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            painter = painterResource(id = R.drawable.ic_copy),
                                                            contentDescription = "Paste",
                                                            tint = funkyColors.ink,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            else -> {
                                                Surface(
                                                    onClick = {
                                                        if (code.length < 6) {
                                                            code += key
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = funkyColors.soft,
                                                    border = BorderStroke(1.dp, funkyColors.line),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(44.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = key,
                                                            fontSize = 18.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = funkyColors.ink
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Join Voice Room CTA Button
                        Button(
                            onClick = {
                                if (code.length != 6) {
                                    Toast.makeText(context, "Please enter all 6 digits!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                onJoin(code)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .shadow(if (code.length == 6) 6.dp else 0.dp, RoundedCornerShape(18.dp), spotColor = funkyColors.mango),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = funkyColors.mango,
                                disabledContainerColor = funkyColors.soft
                            ),
                            enabled = !isLoading && code.length == 6
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = funkyColors.onMango,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Connecting to Room...",
                                    color = funkyColors.onMango,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            } else {
                                Text(
                                    text = "Join Private Room",
                                    color = if (code.length == 6) funkyColors.onMango else funkyColors.mute,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_arrow_right),
                                    contentDescription = null,
                                    tint = if (code.length == 6) funkyColors.onMango else funkyColors.mute,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
