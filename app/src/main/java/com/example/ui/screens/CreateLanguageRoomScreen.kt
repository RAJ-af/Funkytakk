package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.AuthState
import com.example.AuthViewModel
import com.example.ui.components.CircleFlag
import com.example.ui.components.CuteIcon
import com.example.ui.components.CuteIconButton

// Premium high-quality hand-curated Unsplash banners depending on target language
data class QualityBanner(val tag: String, val name: String, val url: String)

val customLanguageBanners = listOf(
    QualityBanner("Japanese", "Sakura Tree", "https://images.unsplash.com/photo-1524413840807-0c3cb6fa808d?w=800"),
    QualityBanner("Japanese", "Tokyo Night", "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=800"),
    QualityBanner("Japanese", "Kyoto Temple", "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=800"),
    
    QualityBanner("English", "London Bridge", "https://images.unsplash.com/photo-1513635269975-59663e0ca1ad?w=800"),
    QualityBanner("English", "New York Skyline", "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800"),
    QualityBanner("English", "Stonehenge Sunset", "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=800"),
    
    QualityBanner("Hindi", "Taj Mahal", "https://images.unsplash.com/photo-1564507592333-c60657eea523?w=800"),
    QualityBanner("Hindi", "Varanasi Ghats", "https://images.unsplash.com/photo-1561361531-99e224e990c3?w=800"),
    QualityBanner("Hindi", "Hawa Mahal", "https://images.unsplash.com/photo-1477587458883-471a5ed08be4?w=800"),
    
    QualityBanner("Spanish", "Barcelona Cathedral", "https://images.unsplash.com/photo-1539650116574-8efeb43e2750?w=800"),
    QualityBanner("Spanish", "Spanish Archways", "https://images.unsplash.com/photo-1485081669829-bacb8c7bb1f3?w=800"),
    
    QualityBanner("French", "Eiffel Tower", "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800"),
    QualityBanner("French", "Louvre Pyramid", "https://images.unsplash.com/photo-1499856871958-5b9627545d1a?w=800"),
    QualityBanner("French", "Provence Lavender", "https://images.unsplash.com/photo-1500382017468-9049fed747ef?w=800"),
    
    QualityBanner("Arabic", "Desert Dunes", "https://images.unsplash.com/photo-1547989453-11e67ffb3885?w=800"),
    QualityBanner("Arabic", "Sheikh Zayed Mosque", "https://images.unsplash.com/photo-1542856391-010fb87dcfed?w=800"),
    
    QualityBanner("Bengali", "Tea Garden", "https://images.unsplash.com/photo-1590001155093-a3c66ab0c3ff?w=800"),
    QualityBanner("Bengali", "Serene River", "https://images.unsplash.com/photo-1583212292454-1fe6229603b7?w=800"),
    
    QualityBanner("Chinese", "Shanghai Bund", "https://images.unsplash.com/photo-1474181487882-5abf3f016c2d?w=800"),
    QualityBanner("Chinese", "Great Wall", "https://images.unsplash.com/photo-1508504509543-191d5ac64987?w=800"),
    
    QualityBanner("Korean", "Seoul Sunset", "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=800"),
    QualityBanner("Korean", "Jeonju Hanok", "https://images.unsplash.com/photo-1538669715515-5c3756c07bae?w=800"),
    
    QualityBanner("Portuguese", "Lisbon Tram", "https://images.unsplash.com/photo-1509840141065-e275f4cf6218?w=800"),
    QualityBanner("Portuguese", "Rio de Janeiro", "https://images.unsplash.com/photo-1483729558449-99ef09a8c325?w=800"),
    
    // Fallbacks
    QualityBanner("All", "Scenic Mountains", "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800"),
    QualityBanner("All", "Golden Shoreline", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800"),
    QualityBanner("All", "Cyberpunk Highway", "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?w=800"),
    QualityBanner("All", "Historic Library", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=800")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLanguageRoomScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onCreate: () -> Unit
) {
    val context = LocalContext.current
    
    // Room entry details
    var roomTitle by remember { mutableStateOf("") }
    var roomDescription by remember { mutableStateOf("") }
    
    // Choose dynamic default language
    val fallbackLanguage = remember {
        LanguageData.popularLanguages.firstOrNull { it.name == "Japanese" }
            ?: LanguageData.popularLanguages.first()
    }
    var selectedLanguage by remember { mutableStateOf(fallbackLanguage) }
    
    // Filter banners based on selected language
    val currentBanners = remember(selectedLanguage) {
        val filtered = customLanguageBanners.filter { it.tag == selectedLanguage.name }
        if (filtered.isNotEmpty()) filtered else customLanguageBanners.filter { it.tag == "All" }
    }
    
    // Default banner URL
    var appliedBannerUrl by remember(selectedLanguage) {
        mutableStateOf(currentBanners.first().url)
    }
    
    // Speaking Level choice
    var selectedLevel by remember { mutableStateOf("Beginner") }
    
    // VIP States & Benefits
    var isHostVip by remember { mutableStateOf(false) }
    var isVipBadgeEnabled by remember { mutableStateOf(false) }
    
    // Overlay sheets
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showVipPaywallSheet by remember { mutableStateOf(false) }
    var vipPlanSelected by remember { mutableStateOf("yearly") } // "monthly" or "yearly"
    
    // User profile connection
    val userProfile = (viewModel.authState.collectAsState().value as? AuthState.Success)?.user
    val userAvatar = userProfile?.avatar ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"
    
    // Set VIP status from firebase user verification state (simulated)
    LaunchedEffect(userProfile) {
        if (userProfile != null) {
            isHostVip = userProfile.isVip
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(Color(0xFFFAFAFA))
            ) {
                // Customized Header: 100% CUTE & SOFT Icons exclusively!
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Soft cute Back Button
                    CuteIconButton(
                        iconName = "back",
                        onClick = onBack,
                        size = 38.dp
                    )

                    // FunkyTalk Brand Tag
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "funkytalk",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "☺",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFFFFC107)
                        )
                    }
                    
                    // Invisible placeholder for balanced alignment
                    Box(modifier = Modifier.size(38.dp))
                }
            }
        },
        bottomBar = {
            // Gold Yellow Floating Create Voice Room Action
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(Color(0xFFFAFAFA))
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        val db = viewModel.firestoreDb
                        val generatedRoomId = "FT${(1001..9999).random()}"
                        val hostId = userProfile?.uid ?: "guest"
                        
                        val newRoomMap = mapOf(
                            "roomId" to generatedRoomId,
                            "title" to roomTitle.ifBlank { "Let's speak in ${selectedLanguage.name}!" },
                            "description" to roomDescription.ifBlank { "Meet new friends and practice speaking ${selectedLanguage.name} together!" },
                            "language" to selectedLanguage.name,
                            "languageCode" to selectedLanguage.flagCode,
                            "languageFlag" to selectedLanguage.flagCode,
                            "bannerImageUrl" to appliedBannerUrl,
                            "cardBackgroundType" to "image",
                            "levelTag" to selectedLevel,
                            "statusTag" to "NEW",
                            "speakingCount" to 1,
                            "listeningCount" to 0,
                            "totalParticipants" to 1,
                            "previewAvatars" to listOf(userAvatar),
                            "status" to "active",
                            "isLive" to true,
                            "hostId" to hostId,
                            "hostIsVip" to (isHostVip && isVipBadgeEnabled),
                            "hostIsVerified" to false,
                            "createdAt" to System.currentTimeMillis(),
                            "lastActivityAt" to System.currentTimeMillis()
                        )

                        if (db != null) {
                            db.collection("rooms").document(generatedRoomId).set(newRoomMap)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "🎈 Room Created Successfully!", Toast.LENGTH_SHORT).show()
                                    onCreate()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Offline saved, creating room!", Toast.LENGTH_SHORT).show()
                                    onCreate()
                                }
                        } else {
                            Toast.makeText(context, "Offline mockup: Created!", Toast.LENGTH_SHORT).show()
                            onCreate()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                    shape = RoundedCornerShape(27.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✨",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = "Create Room",
                            color = Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFFAFAFA)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Character Segment
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Create Room",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Start a voice room and connect with people\nwho love language practice 💛",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            lineHeight = 16.sp
                        )
                    }
                    
                    // Cute mascot circle character
                    Box(
                        modifier = Modifier.size(72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFFFFBEB), CircleShape)
                        )
                        Image(
                            painter = painterResource(id = com.example.R.drawable.language_room_icon_1781320484319),
                            contentDescription = "Mascot Helper",
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        // Tiny talking dot bubble
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(20.dp)
                                .background(Color(0xFFA855F7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💬", fontSize = 11.sp)
                        }
                    }
                }

                // ---------------- STEP 1: Basic Custom Fields ----------------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF3F4F6))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFFFFC107), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "1",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Basic Information",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        }

                        // Room Title
                        Text(
                            text = "Room Title *",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = roomTitle,
                            onValueChange = { if (it.length <= 50) roomTitle = it },
                            placeholder = { Text("e.g. Friendly Japanese Practice 🌸", color = Color.Gray, fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedBorderColor = Color(0xFFFFC107),
                                unfocusedContainerColor = Color(0xFFF9FAFB),
                                focusedContainerColor = Color.White,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            )
                        )
                        Text(
                            text = "${roomTitle.length}/50",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            textAlign = TextAlign.End
                        )

                        // Description
                        Text(
                            text = "Description",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = roomDescription,
                            onValueChange = { if (it.length <= 120) roomDescription = it },
                            placeholder = { Text("Daily friendly chat on hobbies & learning tips!", color = Color.Gray, fontSize = 13.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(75.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedBorderColor = Color(0xFFFFC107),
                                unfocusedContainerColor = Color(0xFFF9FAFB),
                                focusedContainerColor = Color.White,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            )
                        )
                        Text(
                            text = "${roomDescription.length}/120",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            textAlign = TextAlign.End
                        )

                        // Target Language Selector
                        Text(
                            text = "Target Language *",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .background(Color(0xFFF9FAFB), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                                .clickable { showLanguageSheet = true }
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircleFlag(
                                    countryCode = selectedLanguage.flagCode,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = selectedLanguage.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black
                                )
                            }
                            
                            // CUTE soft directional indicator
                            CuteIcon(
                                iconName = "arrow_down",
                                size = 20.dp
                            )
                        }
                    }
                }

                // ---------------- STEP 2: Beautiful Carousel Banners ----------------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF3F4F6))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFFFFC107), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "2",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Select Banner Theme",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        }

                        // Live Selected Banner Image Frame (16:9 aspect ratio preview)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(appliedBannerUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Room Banner Preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            
                            // Top right 16:9 label
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "HD 16:9",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Matching Themes for ${selectedLanguage.name}:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        // horizontal scroll of banners custom selections
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(currentBanners) { banner ->
                                val isSelected = appliedBannerUrl == banner.url
                                Box(
                                    modifier = Modifier
                                        .size(width = 110.dp, height = 75.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color(0xFFFFC107) else Color(0xFFE5E7EB),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { appliedBannerUrl = banner.url }
                                ) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(banner.url)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = banner.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    
                                    // Title label overlay
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.4f))
                                            .padding(vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = banner.name,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Upload from gallery - triggers VIP payment simulated sheets
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFFBEB), RoundedCornerShape(12.dp))
                                .clickable { showVipPaywallSheet = true }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CuteIcon(iconName = "tune", size = 28.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Upload Custom Banner",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.Black
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFFFFC107), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "VIP",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 8.sp,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Import any high-res banner from gallery",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                            CuteIcon(iconName = "lock", size = 24.dp)
                        }
                    }
                }

                // ---------------- STEP 3: Speaking Levels tags ----------------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF3F4F6))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFFFFC107), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "3",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Speaking Level Target",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val levelCards = listOf(
                                Triple("Beginner", "🌱", "Beginner"),
                                Triple("Intermediate", "📊", "Intermediate"),
                                Triple("Advanced", "⭐", "Advanced")
                            )

                            levelCards.forEach { (id, emoji, textStr) ->
                                val isSelected = selectedLevel == id
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedLevel = id },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFFFFBEB) else Color.White
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFFFFC107) else Color(0xFFE5E7EB)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = emoji, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = textStr,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ---------------- STEP 4: VIP Badging ----------------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF3F4F6))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFFFFC107), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "4",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "VIP Room Decorator",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Decorate with VIP tag",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFFFC107), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "VIP",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 8.sp,
                                            color = Color.Black
                                        )
                                    }
                                }
                                Text(
                                    text = "Displays a gorgeous sparkling crown indicator",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            // Native beautiful switcher
                            Switch(
                                checked = isVipBadgeEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (isHostVip) {
                                            isVipBadgeEnabled = true
                                        } else {
                                            showVipPaywallSheet = true
                                        }
                                    } else {
                                        isVipBadgeEnabled = false
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedTrackColor = Color(0xFFFFC107),
                                    checkedThumbColor = Color.White
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }

            // ================= SELECT LANGUAGE SHEET =================
            if (showLanguageSheet) {
                var queryText by remember { mutableStateOf("") }
                val filteredLangs = remember(queryText) {
                    if (queryText.isBlank()) {
                        LanguageData.allLanguages
                    } else {
                        LanguageData.allLanguages.filter {
                            it.name.contains(queryText, ignoreCase = true) ||
                            it.nativeName.contains(queryText, ignoreCase = true)
                        }
                    }
                }

                ModalBottomSheet(
                    onDismissRequest = { showLanguageSheet = false },
                    containerColor = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.82f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Choose Target Language",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color.Black,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        // Search Field
                        OutlinedTextField(
                            value = queryText,
                            onValueChange = { queryText = it },
                            placeholder = { Text("Search by language name...", color = Color.Gray, fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            leadingIcon = {
                                CuteIcon(iconName = "search", size = 20.dp)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedBorderColor = Color(0xFFFFC107),
                                unfocusedContainerColor = Color(0xFFF9FAFB),
                                focusedContainerColor = Color.White
                            )
                        )

                        // Quick popular selections utilizing modern SVG flag components
                        Text(
                            text = "⭐ Popular Languages",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.Black
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val popularLocales = listOf(
                                Triple("Japanese", "jp", "日本語"),
                                Triple("Korean", "kr", "한국어"),
                                Triple("English", "us", "English"),
                                Triple("Spanish", "es", "Español"),
                                Triple("Chinese", "cn", "中文")
                            )

                            popularLocales.forEach { (name, code, native) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable {
                                            selectedLanguage = LanguageData.popularLanguages.firstOrNull { it.name == name }
                                                ?: LanguageData.popularLanguages.first()
                                            showLanguageSheet = false
                                        }
                                        .padding(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(Color(0xFFF9FAFB), RoundedCornerShape(14.dp))
                                            .border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(14.dp))
                                            .padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircleFlag(
                                            countryCode = code,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF3F4F6), thickness = 1.dp)

                        Text(
                            text = "All Supported Languages",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.Black
                        )

                        // Real-time custom lists
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(filteredLangs) { language ->
                                val isSelected = selectedLanguage.name == language.name
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedLanguage = language
                                            showLanguageSheet = false
                                        }
                                        .background(
                                            if (isSelected) Color(0xFFFFFBEB) else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircleFlag(
                                            countryCode = language.flagCode,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = language.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black
                                            )
                                            Text(
                                                text = "${language.nativeName} · ${language.code}",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    
                                    if (isSelected) {
                                        CuteIcon(iconName = "forward", size = 20.dp, badgeColor = Color(0xFFFFC107))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================= PREMIUM VIP PAYWALL SHEET =================
            if (showVipPaywallSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showVipPaywallSheet = false },
                    containerColor = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Decorative Header Crown
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color(0xFFFEF3C7), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👑", fontSize = 38.sp)
                            }
                        }

                        // Pitch Header
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Join FunkyTalk VIP ",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = Color.Black
                                )
                                Text(
                                    text = "VIP",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = Color(0xFFFFC107)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Stand out with custom cards and unlock elite features!\nPractice like a VIP learner 👑",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }

                        // Premium Perks lists
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF9FAFB), RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val perks = listOf(
                                Pair("🎨", "Upload customized room banners"),
                                Pair("👑", "Show VIP royal crown badge on all room cards"),
                                Pair("✨", "Access private chat and unlimited direct streams")
                            )

                            perks.forEach { (emoji, textVal) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .background(Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = textVal,
                                        fontSize = 12.sp,
                                        color = Color.DarkGray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Plans list selections
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Monthly
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { vipPlanSelected = "monthly" },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(
                                    width = if (vipPlanSelected == "monthly") 2.dp else 1.dp,
                                    color = if (vipPlanSelected == "monthly") Color(0xFFFFC107) else Color(0xFFE5E7EB)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    RadioButton(
                                        selected = vipPlanSelected == "monthly",
                                        onClick = { vipPlanSelected = "monthly" },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFC107))
                                    )
                                    Text("Monthly", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("₹249 / mo", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.Black)
                                }
                            }

                            // Yearly
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { vipPlanSelected = "yearly" },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(
                                    width = if (vipPlanSelected == "yearly") 2.dp else 1.dp,
                                    color = if (vipPlanSelected == "yearly") Color(0xFFFFC107) else Color(0xFFE5E7EB)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .background(
                                                Color(0xFFFFC107),
                                                RoundedCornerShape(bottomStart = 8.dp, topEnd = 12.dp)
                                            )
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text("Best", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        RadioButton(
                                            selected = vipPlanSelected == "yearly",
                                            onClick = { vipPlanSelected = "yearly" },
                                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFFFC107))
                                        )
                                        Text("Yearly", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("₹1,999 / yr", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.Black)
                                    }
                                }
                            }
                        }

                        // Unlock checkout payment processing (Simulated)
                        Button(
                            onClick = {
                                viewModel.purchaseVip(context) {
                                    isHostVip = true
                                    isVipBadgeEnabled = true
                                    showVipPaywallSheet = false
                                    Toast.makeText(context, "Welcome! VIP Status Unlocked! 👑✨", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                            shape = RoundedCornerShape(26.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CuteIcon(iconName = "crown", size = 20.dp, badgeColor = Color.Black)
                                Text(
                                    text = "Upgrade to VIP Now",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                                CuteIcon(iconName = "forward", size = 20.dp, badgeColor = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}
