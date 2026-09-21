package com.example.ui.screens.home.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.AuthViewModel
import com.example.UserProfile
import com.example.ui.components.CircleFlag
import com.example.ui.screens.home.models.DarkText
import com.example.ui.screens.home.models.GraySubtitle
import com.example.ui.theme.GoldAccent

@Composable
fun ProfileTabContent(
    currentUserProfile: UserProfile,
    onLogoutClick: () -> Unit,
    viewModel: AuthViewModel? = null
) {
    val context = LocalContext.current
    var showEditProfileDialog by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9FA)),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            // Profile header card with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(GoldAccent.copy(alpha = 0.4f), Color(0xFFF9F9FA))
                        )
                    ),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = 20.dp)
                ) {
                    // Rounded avatar image
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .border(4.dp, Color.White, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                    ) {
                        val avatarVal = currentUserProfile.avatar ?: ""
                        if (avatarVal.isNotEmpty()) {
                            AsyncImage(
                                model = avatarVal,
                                contentDescription = "My Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "Default Avatar",
                                tint = Color.LightGray,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentUserProfile.displayName?.ifBlank { "Unassigned Name" } ?: "FunkyTalk Learner",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "@" + (currentUserProfile.username?.ifBlank { "funky_username" } ?: "learner"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GraySubtitle
                )

                // Render flag and country if populated
                val myCountry = currentUserProfile.country ?: "India"
                val myCode = currentUserProfile.countryCode ?: "in"
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.White)
                        .border(0.5.dp, Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(100.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    CircleFlag(
                        countryCode = myCode,
                        modifier = Modifier.size(16.dp).clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = myCountry,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showEditProfileDialog = true },
                    shape = RoundedCornerShape(100.dp),
                    border = BorderStroke(1.dp, GoldAccent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("edit_profile_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = GoldAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Profile", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Statistic cells row
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val stats = listOf(
                    "24" to "Rooms joined",
                    "18.5h" to "Conversations",
                    "5 days" to "Study Streak"
                )
                stats.forEach { (count, label) ->
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(count, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.Black)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }

        // Onboarding Language choices metadata section
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Language Goals", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Native Language
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF3EDFD)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFF6B21A8), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Native Language", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text(currentUserProfile.nativeLanguage ?: "Not set", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Learning Language
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE6F4EA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF137333), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Learning Target", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text(currentUserProfile.learningLanguage ?: "Not set", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }
        }

        // Onboarding selected Hobbies section
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("My Hobbies", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(12.dp))

                    val hobbiesList = currentUserProfile.hobbies.ifEmpty {
                        listOf("Exchange", "Travel", "Culture", "Anime", "Pop Vocal")
                    }

                    // Wrapping rows of hobby chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        hobbiesList.forEach { hobby ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(Color(0xFFF3F4F6))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(hobby, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            }
                        }
                    }
                }
            }
        }

        // Metadata Details (Gender, Age / DOB)
        item {
            Spacer(modifier = Modifier.height(14.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Personal Details", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Gender
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gender", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Text(currentUserProfile.gender?.replaceFirstChar { it.uppercase() } ?: "Not specified", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Birthday / Date of Birth
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Date of Birth", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Text(currentUserProfile.dob ?: "Not specified", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }

        // Logout Action section - keeping it beautifully positioned in the profile tab!
        item {
            Spacer(modifier = Modifier.height(30.dp))
            Button(
                onClick = onLogoutClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(56.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout from FunkyTalk", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    if (showEditProfileDialog) {
        var editUsername by remember { mutableStateOf((currentUserProfile.username ?: "").lowercase()) }
        var editDisplayName by remember { mutableStateOf(currentUserProfile.displayName ?: "") }
        var isChecking by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = Color.White,
            title = {
                Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = DarkText)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column {
                        Text("Username", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = editUsername,
                            onValueChange = { input ->
                                errorMessage = null
                                editUsername = input.filter { it.isLetterOrDigit() || it == '_' }.lowercase()
                            },
                            placeholder = { Text("e.g. raj_learner", color = Color.Gray) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_username_input"),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(color = DarkText, fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = DarkText,
                                unfocusedTextColor = DarkText,
                                cursorColor = GoldAccent,
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                        Text(
                            text = "Usernames must be unique and automatically lowercased",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Column {
                        Text("Display Name", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = editDisplayName,
                            onValueChange = { editDisplayName = it },
                            placeholder = { Text("Enter your display name", color = Color.Gray) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(color = DarkText, fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = DarkText,
                                unfocusedTextColor = DarkText,
                                cursorColor = GoldAccent,
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanUsername = editUsername.trim().lowercase()
                        if (cleanUsername.isBlank() || editDisplayName.isBlank()) {
                            errorMessage = "Username and display name cannot be blank."
                            return@Button
                        }
                        if (cleanUsername.length < 3) {
                            errorMessage = "Username must be at least 3 characters."
                            return@Button
                        }

                        if (cleanUsername == (currentUserProfile.username ?: "").lowercase()) {
                            if (viewModel != null) {
                                viewModel.updateProfile(
                                    context = context,
                                    username = cleanUsername,
                                    displayName = editDisplayName,
                                    avatar = currentUserProfile.avatar ?: "",
                                    dob = currentUserProfile.dob ?: "",
                                    gender = currentUserProfile.gender ?: "",
                                    nativeLanguage = currentUserProfile.nativeLanguage ?: "",
                                    learningLanguage = currentUserProfile.learningLanguage ?: "",
                                    country = currentUserProfile.country ?: "",
                                    countryCode = currentUserProfile.countryCode ?: "",
                                    hobbies = currentUserProfile.hobbies,
                                    onSuccess = {
                                        Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                                        showEditProfileDialog = false
                                    }
                                )
                            } else {
                                showEditProfileDialog = false
                            }
                        } else {
                            isChecking = true
                            if (viewModel != null) {
                                viewModel.checkUsernameUnique(cleanUsername, currentUserProfile.uid) { isUnique ->
                                    isChecking = false
                                    if (isUnique) {
                                        viewModel.updateProfile(
                                            context = context,
                                            username = cleanUsername,
                                            displayName = editDisplayName,
                                            avatar = currentUserProfile.avatar ?: "",
                                            dob = currentUserProfile.dob ?: "",
                                            gender = currentUserProfile.gender ?: "",
                                            nativeLanguage = currentUserProfile.nativeLanguage ?: "",
                                            learningLanguage = currentUserProfile.learningLanguage ?: "",
                                            country = currentUserProfile.country ?: "",
                                            countryCode = currentUserProfile.countryCode ?: "",
                                            hobbies = currentUserProfile.hobbies,
                                            onSuccess = {
                                                Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                                                showEditProfileDialog = false
                                            }
                                        )
                                    } else {
                                        errorMessage = "This username is already taken. Please pick another."
                                    }
                                }
                            } else {
                                showEditProfileDialog = false
                            }
                        }
                    },
                    enabled = !isChecking,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save Changes", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}
