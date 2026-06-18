package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.example.ui.components.Icon
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrivacyScreen(
    onContinueClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var isAgreed by remember { mutableStateOf(false) }
    val targetUrl = "https://lost39.github.io/funkytalk/#privacy"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Soft glowing yellow/amber circular decorative blob top right
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 60.dp, y = (-60).dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFFBEA))
            )

            // Soft decorative blob bottom left
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-90).dp, y = 90.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFFBEA))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Shield icon badge in top left (matching Screenshot 2 logo)
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFFFFF7E2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Privacy Shield Logo",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // "Your privacy matters"
                    Text(
                        text = "Your privacy\nmatters",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                        lineHeight = 40.sp,
                        letterSpacing = (-1).sp,
                        modifier = Modifier.testTag("privacy_header")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Subheading description
                    Text(
                        text = "We keep things simple and transparent.\nHere's what you should know:",
                        fontSize = 15.sp,
                        color = Color(0xFF5A5C64),
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Column of list items with custom beautiful light backgrounds
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Match item
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFF7E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Match you with the best language partners",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }

                        // 2. Secret messages item
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFF7E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Your messages stay private",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }

                        // 3. Delete account item
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFF7E2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Delete your account and data anytime",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Outbound Document Rows clicking to lost39.github.io
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PolicyRow(
                            title = "Privacy Policy",
                            icon = Icons.Default.Shield,
                            onClick = {
                                uriHandler.openUri(targetUrl)
                            }
                        )

                        PolicyRow(
                            title = "Terms & Conditions",
                            icon = Icons.Default.Description,
                            onClick = {
                                uriHandler.openUri(targetUrl)
                            }
                        )
                    }
                }

                // Consent & Action Bar at the Bottom
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Checkbox Consent Row with customized interactive link style
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAgreed = !isAgreed }
                            .padding(vertical = 8.dp)
                            .testTag("consent_checkbox_row"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isAgreed,
                            onCheckedChange = { isAgreed = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFFFFC529),
                                uncheckedColor = Color(0xFF757575),
                                checkmarkColor = Color.Black
                            ),
                            modifier = Modifier.testTag("privacy_checkbox")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        val annotatedText = buildAnnotatedString {
                            append("I agree to the ")
                            pushStringAnnotation(tag = "URL", annotation = targetUrl)
                            withStyle(style = SpanStyle(color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)) {
                                append("Privacy Policy")
                            }
                            pop()
                            append(" and ")
                            pushStringAnnotation(tag = "URL", annotation = targetUrl)
                            withStyle(style = SpanStyle(color = Color(0xFFFFB300), fontWeight = FontWeight.Bold)) {
                                append("Terms")
                            }
                            pop()
                        }

                        androidx.compose.foundation.text.ClickableText(
                            text = annotatedText,
                            style = LocalTextStyle.current.copy(fontSize = 14.sp, color = Color.Black),
                            onClick = { offset ->
                                annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                    .firstOrNull()?.let { annotation ->
                                        uriHandler.openUri(annotation.item)
                                    } ?: run {
                                        isAgreed = !isAgreed
                                    }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Solid round Continue Button (Disabled if not checked)
                    Button(
                        onClick = {
                            if (isAgreed) {
                                onContinueClick()
                            }
                        },
                        enabled = isAgreed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("privacy_continue_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC529),
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFFE5E5EA),
                            disabledContentColor = Color(0xFF8E8E93)
                        ),
                        shape = RoundedCornerShape(29.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = "Continue",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PolicyRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("policy_row_${title.replace(" ", "_").lowercase()}"),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF7F7F9)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Open Link Icon",
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
