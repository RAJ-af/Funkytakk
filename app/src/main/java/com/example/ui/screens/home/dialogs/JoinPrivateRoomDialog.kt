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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
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
    var code by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

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
                .background(Color.Black.copy(alpha = 0.65f))
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
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                border = BorderStroke(1.5.dp, Color(0xFFFDE68A))
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
                                        Color(0xFFFFFBEB),
                                        Color(0xFFFEF3C7).copy(alpha = 0.4f),
                                        Color.White
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
                            .background(Color.White, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF4B5563),
                            modifier = Modifier.size(18.dp)
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
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFF92400E),
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Category Pill Tag
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, Color(0xFFFCD34D))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color(0xFFB45309),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "PRIVATE VOICE ROOM",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFB45309),
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
                            color = Color(0xFF111827),
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Subtitle
                        Text(
                            text = "Enter the 6-digit passcode shared by the host to connect immediately.",
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // 6-Digit Visual Code Boxes
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    focusRequester.requestFocus()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Invisible text field to capture keyboard input
                            BasicTextField(
                                value = code,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() }
                                    if (filtered.length <= 6) {
                                        code = filtered
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = if (code.length == 6) ImeAction.Done else ImeAction.None
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (code.length == 6 && !isLoading) {
                                            onJoin(code)
                                        }
                                    }
                                ),
                                modifier = Modifier
                                    .focusRequester(focusRequester)
                                    .size(1.dp),
                                singleLine = true
                            )

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
                                        isFilled -> Color(0xFFFFFDF5)
                                        isCurrent -> Color(0xFFFFFBEB)
                                        else -> Color(0xFFF9FAFB)
                                    }

                                    val borderColor = when {
                                        isCurrent -> Color(0xFFFFB800)
                                        isFilled -> Color(0xFFF59E0B)
                                        else -> Color(0xFFE5E7EB)
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
                                                color = Color(0xFF1F2937),
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
                                                    .background(Color(0xFFD97706).copy(alpha = alpha), RoundedCornerShape(1.dp))
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .background(Color(0xFFD1D5DB), CircleShape)
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
                                color = Color(0xFFF3F4F6),
                                border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = null,
                                        tint = Color(0xFF4B5563),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Paste Code",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF374151)
                                    )
                                }
                            }

                            if (code.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(10.dp))
                                Surface(
                                    onClick = { code = "" },
                                    shape = RoundedCornerShape(100.dp),
                                    color = Color(0xFFFEE2E2),
                                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = "Clear",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626),
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
                                                    color = Color(0xFFF3F4F6),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(44.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                            contentDescription = "Backspace",
                                                            tint = Color(0xFF4B5563),
                                                            modifier = Modifier.size(18.dp)
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
                                                    color = Color(0xFFF3F4F6),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(44.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Default.Key,
                                                            contentDescription = "Paste",
                                                            tint = Color(0xFF4B5563),
                                                            modifier = Modifier.size(18.dp)
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
                                                    color = Color(0xFFF8FAFC),
                                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(44.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = key,
                                                            fontSize = 18.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color(0xFF1E293B)
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
                                .shadow(if (code.length == 6) 6.dp else 0.dp, RoundedCornerShape(18.dp), spotColor = GoldAccent),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldAccent,
                                disabledContainerColor = Color(0xFFF3F4F6)
                            ),
                            enabled = !isLoading && code.length == 6
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Connecting to Room...",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            } else {
                                Text(
                                    text = "Join Private Room",
                                    color = if (code.length == 6) Color.Black else Color(0xFF9CA3AF),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = if (code.length == 6) Color.Black else Color(0xFF9CA3AF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
