package com.example.ui.screens.profile.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LocalFunkyColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseAgeSheet(
    initialAge: Int,
    onAgeSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val funkyColors = LocalFunkyColors.current
    var currentAge by remember { mutableStateOf(initialAge.coerceAtLeast(15)) }

    val quickPicks = listOf(18, 21, 25, 28, 32)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = funkyColors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .background(funkyColors.line, RoundedCornerShape(2.dp))
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "Choose your age",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = funkyColors.ink,
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
                        .background(if (currentAge > 15) funkyColors.soft else funkyColors.bg)
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
                            .background(if (currentAge > 15) funkyColors.ink else funkyColors.mute, RoundedCornerShape(2.dp))
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Center Display Card with Yellow Border
                Box(
                    modifier = Modifier
                        .width(130.dp)
                        .height(84.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(funkyColors.soft)
                        .border(1.5.dp, funkyColors.mango, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentAge.toString(),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = funkyColors.ink
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Plus Button
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(funkyColors.soft)
                        .clickable {
                            if (currentAge < 99) {
                                currentAge += 1
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_plus),
                        contentDescription = "Increase age",
                        tint = funkyColors.ink,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Age Restriction Notice
            Text(
                text = "Must be at least 15 years old to use FunkyTalk",
                fontSize = 12.sp,
                color = if (currentAge == 15) funkyColors.mango else funkyColors.mute,
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
                    color = funkyColors.mute,
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
                                .background(if (isSelected) funkyColors.mango else funkyColors.bg)
                                .border(1.dp, if (isSelected) funkyColors.mango else funkyColors.line, RoundedCornerShape(14.dp))
                                .clickable {
                                    currentAge = agePick
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = agePick.toString(),
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) funkyColors.onMango else funkyColors.ink
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
                    containerColor = funkyColors.mango,
                    contentColor = funkyColors.onMango
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
                    color = funkyColors.onMango
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
                    color = funkyColors.mute
                )
            }
        }
    }
}
