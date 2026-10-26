package com.example.ui.screens.profile.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TextPrimary = Color(0xFF111827)
private val TextSecondary = Color(0xFF4B5563)
private val PrimaryYellow = Color(0xFFF8C63D)
private val CardBg = Color(0xFFFFFDF5)
private val CircleBtnBg = Color(0xFFF3F4F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseAgeSheet(
    initialAge: Int,
    onAgeSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentAge by remember { mutableStateOf(initialAge.coerceAtLeast(15)) }

    val quickPicks = listOf(18, 21, 25, 28, 32)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .background(Color(0xFFE5E7EB), RoundedCornerShape(2.dp))
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "Choose your age",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Age Counter Row: [-]  [  21  ]  [+]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Minus Button
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (currentAge > 15) CircleBtnBg else Color(0xFFF9FAFB))
                        .clickable(enabled = currentAge > 15) {
                            if (currentAge > 15) {
                                currentAge -= 1
                            } else {
                                Toast.makeText(context, "Minimum age requirement is 15 years", Toast.LENGTH_SHORT).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(2.5.dp)
                            .background(if (currentAge > 15) TextPrimary else Color(0xFF9CA3AF), RoundedCornerShape(2.dp))
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Center Display Card with Yellow Border
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(84.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardBg)
                        .border(1.5.dp, PrimaryYellow, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentAge.toString(),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Plus Button
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(CircleBtnBg)
                        .clickable {
                            if (currentAge < 99) {
                                currentAge += 1
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase age",
                        tint = TextPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Age Restriction Notice
            Text(
                text = "Must be at least 15 years old to use FunkyTalk",
                fontSize = 12.sp,
                color = if (currentAge == 15) Color(0xFFD97706) else TextSecondary,
                modifier = Modifier.padding(top = 10.dp, bottom = 20.dp)
            )

            // Quick picks row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Quick picks",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    quickPicks.forEach { agePick ->
                        val isSelected = currentAge == agePick
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) PrimaryYellow else Color(0xFFF3F4F6))
                                .clickable {
                                    currentAge = agePick
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = agePick.toString(),
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Save age Full-Width Yellow Button
            Button(
                onClick = {
                    if (currentAge >= 15) {
                        onAgeSelected(currentAge)
                        onDismiss()
                    } else {
                        Toast.makeText(context, "Users under 15 years cannot register", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryYellow,
                    contentColor = TextPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Save age",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Cancel Button
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 12.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
        }
    }
}
