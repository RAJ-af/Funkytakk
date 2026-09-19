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
import com.example.UserProfile
import com.example.ui.components.CircleFlag
import com.example.ui.screens.home.models.GraySubtitle
import com.example.ui.theme.GoldAccent

@Composable
fun ProfileTabContent(
    currentUserProfile: UserProfile,
    onLogoutClick: () -> Unit
) {
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
}
