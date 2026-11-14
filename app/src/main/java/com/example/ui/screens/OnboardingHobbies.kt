package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.components.FunkyLockIcon
import com.example.ui.components.SoftCloseIcon
import com.example.ui.components.SoftHeartIcon
import com.example.ui.components.SoftPlusIcon
import com.example.ui.theme.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val predefinedHobbies = listOf(
    "Music", "Travel", "Reading", "Gaming",
    "Cooking", "Photography", "Sports", "Fitness",
    "Art", "Movies", "Writing", "Dancing",
    "Technology", "Nature", "Pets", "Learning"
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingHobbiesScreen(
    initialHobbies: List<String> = emptyList(),
    onComplete: (List<String>) -> Unit,
    onBack: () -> Unit
) {
    val funkyColors = LocalFunkyColors.current
    val selectedHobbies = remember {
        mutableStateListOf<String>().apply {
            addAll(initialHobbies)
        }
    }
    var customHobby by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            OnboardingStepIndicator(
                currentStep = 7,
                totalSteps = 7,
                onBackClick = onBack
            )
        },
        containerColor = funkyColors.bg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(funkyColors.soft, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        SoftHeartIcon(
                            isLiked = true,
                            size = 40.dp,
                            color = funkyColors.mango
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "What are your hobbies?",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = funkyColors.ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Select at least 3 that describe you best",
                        fontSize = 14.sp,
                        color = funkyColors.mute,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    val chunks = predefinedHobbies.chunked(3)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (chunk in chunks) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (hobby in chunk) {
                                    val isSelected = selectedHobbies.contains(hobby)
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                if (isSelected) {
                                                    selectedHobbies.remove(hobby)
                                                } else {
                                                    if (selectedHobbies.size < 20) {
                                                        selectedHobbies.add(hobby)
                                                    }
                                                }
                                            },
                                        colors = CardDefaults.cardColors(containerColor = if (isSelected) funkyColors.soft else funkyColors.surface),
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) funkyColors.mango else funkyColors.line
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = hobby.split(" ").firstOrNull() ?: hobby,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = funkyColors.ink
                                            )
                                        }
                                    }
                                }
                                if (chunk.size < 3) {
                                    repeat(3 - chunk.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Selected (${selectedHobbies.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.mute
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                if (selectedHobbies.isNotEmpty()) {
                    item {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedHobbies.forEach { hobby ->
                                Card(
                                    modifier = Modifier.clickable { selectedHobbies.remove(hobby) },
                                    colors = CardDefaults.cardColors(containerColor = funkyColors.soft),
                                    shape = RoundedCornerShape(100.dp),
                                    border = BorderStroke(1.dp, funkyColors.mango.copy(alpha = 0.55f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(text = hobby, fontSize = 13.sp, color = funkyColors.ink, fontWeight = FontWeight.Medium)
                                        SoftCloseIcon(size = 14.dp, color = funkyColors.mute)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                item {
                    OutlinedTextField(
                        value = customHobby,
                        onValueChange = { customHobby = it },
                        placeholder = { Text("Add custom hobby...", color = funkyColors.mute, fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = funkyColors.ink, fontSize = 14.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = funkyColors.ink,
                            unfocusedTextColor = funkyColors.ink,
                            cursorColor = funkyColors.mango,
                            focusedBorderColor = funkyColors.mango,
                            unfocusedBorderColor = funkyColors.line,
                            focusedContainerColor = funkyColors.surface,
                            unfocusedContainerColor = funkyColors.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (customHobby.isNotBlank() && selectedHobbies.size < 20 && !selectedHobbies.contains(customHobby.trim())) {
                                    selectedHobbies.add(customHobby.trim())
                                    customHobby = ""
                                }
                            }
                        ),
                        trailingIcon = {
                            IconButton(onClick = {
                                if (customHobby.isNotBlank() && selectedHobbies.size < 20 && !selectedHobbies.contains(customHobby.trim())) {
                                    selectedHobbies.add(customHobby.trim())
                                    customHobby = ""
                                }
                            }) {
                                SoftPlusIcon(
                                    size = 20.dp,
                                    color = if (customHobby.isNotBlank()) funkyColors.mango else funkyColors.mute
                                )
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FunkyLockIcon(
                            color = funkyColors.mute,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "This helps us match you with like-minded people. You can change this later.",
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.mute
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Button(
                onClick = {
                    if (selectedHobbies.size >= 3) {
                        onComplete(selectedHobbies.toList())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(vertical = 4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = funkyColors.mango,
                    contentColor = funkyColors.onMango,
                    disabledContainerColor = funkyColors.soft,
                    disabledContentColor = funkyColors.mute
                ),
                enabled = selectedHobbies.size >= 3,
                shape = RoundedCornerShape(100.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Complete Profile",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
