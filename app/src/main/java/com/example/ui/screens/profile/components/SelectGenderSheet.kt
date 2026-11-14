package com.example.ui.screens.profile.components

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.screens.ProfileGenderFemaleIcon
import com.example.ui.screens.ProfileGenderMaleIcon
import com.example.ui.theme.LocalFunkyColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectGenderSheet(
    initialGender: String,
    onGenderSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    var selectedGender by remember { mutableStateOf(if (initialGender.equals("Female", ignoreCase = true)) "Female" else "Male") }

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
                text = "Select gender",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = funkyColors.ink,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 20.dp)
            )

            // Option 1: Male
            val isMale = selectedGender == "Male"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isMale) funkyColors.soft else funkyColors.bg)
                    .border(
                        width = 1.5.dp,
                        color = if (isMale) funkyColors.mango else funkyColors.line,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { selectedGender = "Male" }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (isMale) funkyColors.mango else funkyColors.surface,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        ProfileGenderMaleIcon(
                            modifier = Modifier.size(22.dp),
                            color = if (isMale) funkyColors.onMango else funkyColors.ink
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Male",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = funkyColors.ink
                    )
                }

                // Checkmark indicator
                if (isMale) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(funkyColors.mango, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = "Selected",
                            tint = funkyColors.onMango,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(1.5.dp, funkyColors.line, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Option 2: Female
            val isFemale = selectedGender == "Female"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isFemale) funkyColors.soft else funkyColors.bg)
                    .border(
                        width = 1.5.dp,
                        color = if (isFemale) funkyColors.mango else funkyColors.line,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { selectedGender = "Female" }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (isFemale) funkyColors.mango else funkyColors.surface,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        ProfileGenderFemaleIcon(
                            modifier = Modifier.size(22.dp),
                            color = if (isFemale) funkyColors.onMango else funkyColors.ink
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Female",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = funkyColors.ink
                    )
                }

                // Checkmark indicator
                if (isFemale) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(funkyColors.mango, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_check),
                            contentDescription = "Selected",
                            tint = funkyColors.onMango,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(1.5.dp, funkyColors.line, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Done Button
            Button(
                onClick = {
                    onGenderSelected(selectedGender)
                    onDismiss()
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
                    text = "Done",
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
