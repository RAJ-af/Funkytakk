package com.example.ui.screens.profile.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.ui.theme.LocalFunkyColors

@Composable
fun AddCustomInterestDialog(
    onInterestAdded: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val funkyColors = LocalFunkyColors.current
    var interestText by remember { mutableStateOf("") }
    val maxChars = 30

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = funkyColors.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .border(1.dp, funkyColors.line, RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Sparkle Badge
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(funkyColors.mango.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_sparkle),
                        contentDescription = "Sparkle",
                        tint = funkyColors.mango,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Title
                Text(
                    text = "Add a custom interest",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = funkyColors.ink
                )

                // Subtitle
                Text(
                    text = "Add a hobby, topic, or learning goal.",
                    fontSize = 13.sp,
                    color = funkyColors.mute,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                // Input field with tag icon & character counter
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(funkyColors.bg, RoundedCornerShape(14.dp))
                            .border(1.dp, funkyColors.line, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_hash),
                                contentDescription = "Tag",
                                tint = funkyColors.mute,
                                modifier = Modifier.size(18.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            BasicTextField(
                                value = interestText,
                                onValueChange = {
                                    if (it.length <= maxChars) interestText = it
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    color = funkyColors.ink,
                                    fontWeight = FontWeight.Normal
                                ),
                                cursorBrush = SolidColor(funkyColors.mango),
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    if (interestText.isEmpty()) {
                                        Text(
                                            text = "e.g. Astronomy, Anime, Poetry",
                                            style = TextStyle(
                                                fontSize = 14.sp,
                                                color = funkyColors.mute
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }

                    // Counter
                    Text(
                        text = "${interestText.length}/$maxChars",
                        fontSize = 12.sp,
                        color = funkyColors.mute,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 6.dp, bottom = 20.dp)
                    )
                }

                // Add interest button
                Button(
                    onClick = {
                        val trimmed = interestText.trim()
                        if (trimmed.isNotBlank()) {
                            onInterestAdded(trimmed)
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Please enter an interest", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = funkyColors.mango,
                        contentColor = funkyColors.onMango
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Add interest",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = funkyColors.onMango
                    )
                }

                // Cancel button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = funkyColors.mute
                    )
                }
            }
        }
    }
}
