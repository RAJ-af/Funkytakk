package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StartupPrefsStore
import com.example.ui.components.FunkyLockIcon
import com.example.ui.components.SoftDocumentIcon
import com.example.ui.components.SoftGroupIcon
import com.example.ui.components.SoftOpenInNewIcon
import com.example.ui.components.SoftShieldIcon
import com.example.ui.components.SoftTrashIcon
import com.example.ui.theme.LocalFunkyColors
import kotlinx.coroutines.launch

@Composable
fun PrivacyScreen(
    onContinueClick: () -> Unit,
    onBackClick: () -> Unit = {}
) {
    BackHandler { onBackClick() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val funkyColors = LocalFunkyColors.current
    var isAgreed by remember {
        mutableStateOf(StartupPrefsStore.getFastSnapshot(context).privacyAccepted)
    }
    val targetUrl = "https://lost39.github.io/funkytalk/#privacy"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = funkyColors.bg
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
                    .background(funkyColors.soft.copy(alpha = 0.65f))
            )

            // Soft decorative blob bottom left
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-90).dp, y = 90.dp)
                    .clip(CircleShape)
                    .background(funkyColors.soft.copy(alpha = 0.65f))
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

                    // Shield icon badge in top left
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(funkyColors.soft),
                        contentAlignment = Alignment.Center
                    ) {
                        SoftShieldIcon(
                            size = 32.dp,
                            color = funkyColors.mango
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // "Your privacy matters"
                    Text(
                        text = "Your privacy\nmatters",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = funkyColors.ink,
                        lineHeight = 40.sp,
                        letterSpacing = (-1).sp,
                        modifier = Modifier.testTag("privacy_header")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Subheading description
                    Text(
                        text = "We keep things simple and transparent.\nHere's what you should know:",
                        fontSize = 15.sp,
                        color = funkyColors.mute,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    // Column of list items
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
                                    .background(funkyColors.soft),
                                contentAlignment = Alignment.Center
                            ) {
                                SoftGroupIcon(
                                    size = 20.dp,
                                    color = funkyColors.mango
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Match you with the best language partners",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = funkyColors.ink
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
                                    .background(funkyColors.soft),
                                contentAlignment = Alignment.Center
                            ) {
                                FunkyLockIcon(
                                    color = funkyColors.mango,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Your messages stay private",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = funkyColors.ink
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
                                    .background(funkyColors.soft),
                                contentAlignment = Alignment.Center
                            ) {
                                SoftTrashIcon(
                                    size = 20.dp,
                                    color = funkyColors.mango
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Delete your account and data anytime",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = funkyColors.ink
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
                            iconContent = {
                                SoftShieldIcon(
                                    size = 20.dp,
                                    color = funkyColors.mango
                                )
                            },
                            onClick = {
                                uriHandler.openUri(targetUrl)
                            }
                        )

                        PolicyRow(
                            title = "Terms & Conditions",
                            iconContent = {
                                SoftDocumentIcon(
                                    size = 20.dp,
                                    color = funkyColors.mango
                                )
                            },
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
                                checkedColor = funkyColors.mango,
                                uncheckedColor = funkyColors.mute,
                                checkmarkColor = funkyColors.onMango
                            ),
                            modifier = Modifier.testTag("privacy_checkbox")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        val annotatedText = buildAnnotatedString {
                            append("I agree to the ")
                            pushStringAnnotation(tag = "URL", annotation = targetUrl)
                            withStyle(style = SpanStyle(color = funkyColors.mango, fontWeight = FontWeight.Bold)) {
                                append("Privacy Policy")
                            }
                            pop()
                            append(" and ")
                            pushStringAnnotation(tag = "URL", annotation = targetUrl)
                            withStyle(style = SpanStyle(color = funkyColors.mango, fontWeight = FontWeight.Bold)) {
                                append("Terms")
                            }
                            pop()
                        }

                        androidx.compose.foundation.text.ClickableText(
                            text = annotatedText,
                            style = LocalTextStyle.current.copy(fontSize = 14.sp, color = funkyColors.ink),
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
                                coroutineScope.launch {
                                    StartupPrefsStore.markPrivacyAccepted(context)
                                    onContinueClick()
                                }
                            }
                        },
                        enabled = isAgreed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("privacy_continue_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = funkyColors.mango,
                            contentColor = funkyColors.onMango,
                            disabledContainerColor = funkyColors.soft,
                            disabledContentColor = funkyColors.mute
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
    iconContent: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("policy_row_${title.replace(" ", "_").lowercase()}"),
        colors = CardDefaults.cardColors(
            containerColor = funkyColors.surface
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
                iconContent()
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = funkyColors.ink
                )
            }

            SoftOpenInNewIcon(
                size = 18.dp,
                color = funkyColors.mute
            )
        }
    }
}
