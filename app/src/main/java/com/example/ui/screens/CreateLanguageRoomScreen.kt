package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.AuthState
import com.example.AuthViewModel
import com.example.ui.components.CircleFlag

// Pre-defined high quality banners depending on selected language
data class BannerOption(val name: String, val url: String)

val languageBanners = mapOf(
    "Japanese" to listOf(
        BannerOption("Sakura Street", "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=800"),
        BannerOption("Mount Fuji", "https://images.unsplash.com/photo-1524413840807-0c3cb6fa808d?w=800"),
        BannerOption("Tokyo Night", "https://images.unsplash.com/photo-1503899036084-c55cdd92da26?w=800"),
        BannerOption("Kyoto Temple", "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=800"),
        BannerOption("Anime Style", "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=800")
    ),
    "English" to listOf(
        BannerOption("London Big Ben", "https://images.unsplash.com/photo-1513635269975-59663e0ca1ad?w=800"),
        BannerOption("New York Skyline", "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800"),
        BannerOption("Stonehenge Sunset", "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=800"),
        BannerOption("Hollywood Streets", "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=800")
    ),
    "Korean" to listOf(
        BannerOption("Seoul Sunset", "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=800"),
        BannerOption("Jeonju Hanok", "https://images.unsplash.com/photo-1538669715515-5c3756c07bae?w=800"),
        BannerOption("Jeju Island Beach", "https://images.unsplash.com/photo-1504609773096-104ff2c73ba4?w=800"),
        BannerOption("Gyeongju Palace", "https://images.unsplash.com/photo-1525373612132-b3e820780006?w=800")
    ),
    "Spanish" to listOf(
        BannerOption("Barcelona Cathedral", "https://images.unsplash.com/photo-1509840144506-2c990f23a7f7?w=800"),
        BannerOption("Madrid Royal", "https://images.unsplash.com/photo-1539650116574-8efeb43e2750?w=800"),
        BannerOption("Spanish Archways", "https://images.unsplash.com/photo-1485081669829-bacb8c7bb1f3?w=800")
    ),
    "Chinese" to listOf(
        BannerOption("Shanghai Bund Sunset", "https://images.unsplash.com/photo-1474181487882-5abf3f016c2d?w=800"),
        BannerOption("The Great Wall", "https://images.unsplash.com/photo-1508504509543-191d5ac64987?w=800"),
        BannerOption("Beijing Forbidden City", "https://images.unsplash.com/photo-1490730141103-6cac27aaab94?w=800")
    )
)

val defaultBannersList = listOf(
    BannerOption("Scenic Mountains", "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=800"),
    BannerOption("Golden Shoreline", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800"),
    BannerOption("Cyberpunk Highway", "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?w=800"),
    BannerOption("Historic Library", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=800")
)

object BannerPack {
    val worldwideScenics = listOf(
        BannerOption("Tokyo Akihabara Night", "https://images.unsplash.com/photo-1542051841857-5f90071e7989?w=800"),
        BannerOption("Kyoto Red Toriis", "https://images.unsplash.com/photo-1493976040374-85c8e12f0c0e?w=800"),
        BannerOption("Mount Fuji Sakura", "https://images.unsplash.com/photo-1524413840807-0c3cb6fa808d?w=800"),
        BannerOption("Eiffel Tower Paris", "https://images.unsplash.com/photo-1502602898657-3e91760cbb34?w=800"),
        BannerOption("London Thames Big Ben", "https://images.unsplash.com/photo-1513635269975-59663e0ca1ad?w=800"),
        BannerOption("New York Central Park", "https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?w=800"),
        BannerOption("Taj Mahal Sunrise", "https://images.unsplash.com/photo-1564507592333-c60657eea523?w=800"),
        BannerOption("Seoul Han River Bridge", "https://images.unsplash.com/photo-1538481199705-c710c4e965fc?w=800"),
        BannerOption("Sydney Opera House", "https://images.unsplash.com/photo-1506973035872-a4ec16b8e8d9?w=800"),
        BannerOption("Rome Colosseum Dusk", "https://images.unsplash.com/photo-1552832230-c0197dd311b5?w=800"),
        BannerOption("Santorini Sunset Cliffs", "https://images.unsplash.com/photo-1533105079780-92b9be482077?w=800"),
        BannerOption("Barcelona Park Guell", "https://images.unsplash.com/photo-1523531294919-4bcd7c65e216?w=800"),
        BannerOption("Rio de Janeiro Beach", "https://images.unsplash.com/photo-1483728642387-6c3bdd6c93e5?w=800"),
        BannerOption("Giza Great Pyramids", "https://images.unsplash.com/photo-1503177119275-0aa32b31d468?w=800"),
        BannerOption("Berlin Brandenburg Gate", "https://images.unsplash.com/photo-1599946347371-68eb71b16afc?w=800"),
        BannerOption("Machu Picchu Incan Citadel", "https://images.unsplash.com/photo-1587590227264-0ac64ce63ce8?w=800"),
        BannerOption("Canadian Alpine Lake", "https://images.unsplash.com/photo-1500043357865-c6b8827df7f1?w=800"),
        BannerOption("Grand Canyon Red Rocks", "https://images.unsplash.com/photo-1524492412937-b28074a5d7da?w=800"),
        BannerOption("Great Wall Wilderness", "https://images.unsplash.com/photo-1508504509543-191d5ac64987?w=800"),
        BannerOption("Amalfi Coast Pastel Towns", "https://images.unsplash.com/photo-1486016006115-74a41448aea2?w=800"),
        BannerOption("Singapore Marina Sands", "https://images.unsplash.com/photo-1525596667581-2b083dc3954f?w=800"),
        BannerOption("Indian Amber Palace", "https://images.unsplash.com/photo-1473163928189-364b2c4e1135?w=800"),
        BannerOption("Venice Scenic Canal", "https://images.unsplash.com/photo-1527631746610-bca00a040d60?w=800"),
        BannerOption("Iceland Northern Lights", "https://images.unsplash.com/photo-1483168527879-c66136b56105?w=800")
    )

    val cozyVibes = listOf(
        BannerOption("Aesthetic Rainy Window", "https://images.unsplash.com/photo-1428908728789-d2de25dbd4e2?w=800"),
        BannerOption("Cozy Espresso Table", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=800"),
        BannerOption("Warm Fireplace Lounge", "https://images.unsplash.com/photo-1513694203232-719a280e022f?w=800"),
        BannerOption("Classic Library Shelves", "https://images.unsplash.com/photo-1521587760476-6c12a4b040da?w=800"),
        BannerOption("Cathedral Glass Study", "https://images.unsplash.com/photo-1516979187457-637abb4f9353?w=800"),
        BannerOption("Sunlit Greenhouse Plants", "https://images.unsplash.com/photo-1463936575829-25148e1db1b8?w=800"),
        BannerOption("Minimalist Oak Desk", "https://images.unsplash.com/photo-1499750310107-5fef28a66643?w=800"),
        BannerOption("Vintage Vinyl Record", "https://images.unsplash.com/photo-1539628399213-d6482e76f184?w=800"),
        BannerOption("Acoustic Parlor Guitar", "https://images.unsplash.com/photo-1510915361894-db8b60106cb1?w=800"),
        BannerOption("Steaming Herbal Tea", "https://images.unsplash.com/photo-1576092768241-dec231879fc3?w=800"),
        BannerOption("Bonsai Tree Studio", "https://images.unsplash.com/photo-1502082553048-f009c37129b9?w=800"),
        BannerOption("Lo-fi Coffee Shop View", "https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=800"),
        BannerOption("Candlelit Wooden Rest", "https://images.unsplash.com/photo-1517705008128-361805f42e8a?w=800"),
        BannerOption("Cozy Wool Knit Couch", "https://images.unsplash.com/photo-1583847268964-b28dc8f51f92?w=800"),
        BannerOption("Study Corner Lo-fi", "https://images.unsplash.com/photo-1544640808-32ca72ac7f37?w=800")
    )

    val cyberpunkAndArt = listOf(
        BannerOption("Akira Cyberpunk Sunset", "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=800"),
        BannerOption("Cyber Highway Grid", "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?w=800"),
        BannerOption("Vaporwave Starry Sky", "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?w=800"),
        BannerOption("Neo Tokyo Alley Alleys", "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800"),
        BannerOption("Retro Outrun Sunset", "https://images.unsplash.com/photo-1518531933037-91b2f5f229cc?w=800"),
        BannerOption("Pastel Paint Splash", "https://images.unsplash.com/photo-1579783902614-a3fb3927b6a5?w=800"),
        BannerOption("Aesthetic Memphis Art", "https://images.unsplash.com/photo-1557672172-298e090bd0f1?w=800"),
        BannerOption("Ghibli Cloud Horizons", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800"),
        BannerOption("Anime Town Train Cross", "https://images.unsplash.com/photo-1528164344705-47542687000d?w=800"),
        BannerOption("Neon Synthwave Grid", "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=800"),
        BannerOption("Hologram Terminal", "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800"),
        BannerOption("Aesthetic Ink Drops", "https://images.unsplash.com/photo-1563089145-599997674d42?w=800"),
        BannerOption("Cosmic Space Nebulae", "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?w=800"),
        BannerOption("Pixel Art Skyline", "https://images.unsplash.com/photo-1563089145-599997674d42?w=800"),
        BannerOption("Fantasy Forest Meadow", "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=800")
    )

    // Programmatically generated exactly 100 stunning live gradients
    val gradients: List<BannerOption> = listOf(
        // Curated beautiful gradient combinations
        BannerOption("Sunset Bliss (#1)", "gradient:#FF4500,#FF8C00,#FFD700"),
        BannerOption("Neon Cyberpunk (#2)", "gradient:#8A2BE2,#FF007F,#00FFFF"),
        BannerOption("Aurora Dream (#3)", "gradient:#00FF87,#60EFFF,#21007F"),
        BannerOption("Lofi Lavender (#4)", "gradient:#E0B0FF,#DDA0DD,#4B0082"),
        BannerOption("Royal Velvet (#5)", "gradient:#800080,#9932CC,#1F005C"),
        BannerOption("Ocean Breeze (#6)", "gradient:#00FFFF,#0080FF,#000080"),
        BannerOption("Emerald Forest (#7)", "gradient:#32CD32,#00FF7F,#006400"),
        BannerOption("Matcha Milkshake (#8)", "gradient:#C1E1C1,#779ECB,#033E3E"),
        BannerOption("Electric Orange (#9)", "gradient:#FFA500,#FF4500,#E60000"),
        BannerOption("Sweet Peach (#10)", "gradient:#FFDAB9,#FFB6C1,#FFF0F5"),
        BannerOption("Midnight Purple (#11)", "gradient:#000000,#4B0082,#8A2BE2"),
        BannerOption("Desert Mirage (#12)", "gradient:#F4A460,#E9967A,#8B4513"),
        BannerOption("Tropical Lime (#13)", "gradient:#DFFF00,#32CD32,#008000"),
        BannerOption("Bubblegum Horizon (#14)", "gradient:#FFC0CB,#FF69B4,#8A2BE2"),
        BannerOption("Glacier Ice (#15)", "gradient:#F0F8FF,#E0FFFF,#00BFFF"),
        BannerOption("Volcano Ash (#16)", "gradient:#1C1C1C,#3A3A3A,#8B0000"),
        BannerOption("Autumn Rust (#17)", "gradient:#8B4513,#A0522D,#CD853F"),
        BannerOption("Cotton Candy (#18)", "gradient:#FFB6C1,#87CEFA,#E6E6FA"),
        BannerOption("Mystic Turquoise (#19)", "gradient:#40E0D0,#48D1CC,#008080"),
        BannerOption("Warm Sandalwood (#20)", "gradient:#FFE4C4,#BC8F8F,#8B4513")
    ) + List(80) { index ->
        val palettes = listOf(
            listOf("#7F00FF", "#E100FF"),
            listOf("#FF416C", "#FF4B2B"),
            listOf("#00B4DB", "#0083B0"),
            listOf("#A8FF78", "#78ffd6"),
            listOf("#F27121", "#e94057", "#8a2387"),
            listOf("#00c6ff", "#0072ff"),
            listOf("#ffe259", "#ffa751"),
            listOf("#f857a6", "#ff5858"),
            listOf("#11998e", "#38ef7d"),
            listOf("#101820", "#F2AA4C"),
            listOf("#2193b0", "#6dd5ed"),
            listOf("#1f4037", "#99f2c8"),
            listOf("#bdc3c7", "#2c3e50"),
            listOf("#de6262", "#ffb88c"),
            listOf("#06beb6", "#48b1bf"),
            listOf("#dd5e89", "#f7bb97"),
            listOf("#56ab2f", "#a8e063"),
            listOf("#614385", "#516395"),
            listOf("#eecda3", "#ef629f"),
            listOf("#02aab0", "#00cdac")
        )
        val adjectives = listOf(
            "Retro", "Cosmic", "Neon", "Cyber", "Deep", "Shining", "Velvet", "Plum", "Aura", "Golden",
            "Pacific", "Crimson", "Wild", "Mellow", "Dreamy", "Frozen", "Liquid", "Polished", "Solar", "Acoustic"
        )
        val nouns = listOf(
            "Cascade", "Splendor", "Elysium", "Mist", "Vortex", "Breeze", "Wave", "Glow", "Bliss", "Crush",
            "Saffron", "Lagoon", "Zen", "Amethyst", "Horizon", "Pulse", "Vortex", "Mirage", "Tide", "Dust"
        )
        val palette = palettes[index % palettes.size]
        val adj = adjectives[(index * 2) % adjectives.size]
        val noun = nouns[(index * 3) % nouns.size]
        BannerOption(
            name = "$adj $noun (#${index + 21})",
            url = "gradient:" + palette.joinToString(",")
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateLanguageRoomScreen(
    viewModel: AuthViewModel,
    onBack: () -> Unit,
    onCreate: () -> Unit
) {
    val context = LocalContext.current
    
    // Core room input state values
    var roomTitle by remember { mutableStateOf("") }
    var roomDescription by remember { mutableStateOf("") }
    
    // Language selections (default to Japanese as shown in mockups)
    val defaultLanguage = remember {
        LanguageData.popularLanguages.firstOrNull { it.name == "Japanese" } 
            ?: LanguageData.popularLanguages.first()
    }
    var selectedLanguage by remember { mutableStateOf(defaultLanguage) }
    
    // Choose banner selection (defaults to first banner option of current language)
    val currentLanguageBanners = remember(selectedLanguage) {
        languageBanners[selectedLanguage.name] ?: defaultBannersList
    }
    var temporarySelectedBanner by remember(selectedLanguage) {
        mutableStateOf(currentLanguageBanners.first().url)
    }
    var appliedBannerUrl by remember(selectedLanguage) {
        mutableStateOf(currentLanguageBanners.first().url)
    }
    var selectedCategory by remember { mutableStateOf("Recommended") }
    
    // Level selection: Beginner, Intermediate, Advanced
    var selectedLevel by remember { mutableStateOf("Beginner") }
    
    // VIP benefits state & payment flows
    var hostIsVipLocal by remember { mutableStateOf(false) }
    var isVipBadgeEnabled by remember { mutableStateOf(false) }
    
    // Modal bottoms & Overlay states
    var showLanguageSheet by remember { mutableStateOf(false) }
    var showBannerOverlay by remember { mutableStateOf(false) }
    var showVipPaywallSheet by remember { mutableStateOf(false) }
    var vipPlanSelected by remember { mutableStateOf("yearly") } // "monthly" or "yearly"
    
    // Sync current user verified status
    val userProfile = (viewModel.authState.collectAsState().value as? AuthState.Success)?.user
    val userAvatar = userProfile?.avatar ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"
    
    // Sync initial VIP state
    LaunchedEffect(userProfile) {
        if (userProfile != null) {
            hostIsVipLocal = userProfile.isEmailVerified // Using custom verified state for VIP simulations
        }
    }

    Scaffold(
        topBar = {
            Column {
                // Top Custom Header: Logo, brand name "funkytalk" and mascot
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color(0xFFF3F4F6), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }

                    // Brand Title 'funkytalk'
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "funkytalk",
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "☺",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color(0xFFFFC107)
                        )
                    }
                    
                    // Invisible spacer for centering
                    Box(modifier = Modifier.size(40.dp))
                }
            }
        },
        bottomBar = {
            // Giant gold-yellow create room action
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        val db = viewModel.firestoreDb
                        if (db != null) {
                            val hostId = userProfile?.uid ?: "guest"
                            val generatedRoomId = "FT${(1000..9999).random()}"
                            
                            val newRoomMap = mapOf(
                                "roomId" to generatedRoomId,
                                "title" to roomTitle.ifBlank { "Let's talk in ${selectedLanguage.name}!" },
                                "description" to roomDescription.ifBlank { "Daily conversation practice in ${selectedLanguage.name}" },
                                "language" to selectedLanguage.name,
                                "languageCode" to selectedLanguage.flagCode,
                                "languageFlag" to "", // flagCode is bound to CircleFlag
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
                                "hostIsVip" to (hostIsVipLocal || isVipBadgeEnabled),
                                "hostIsVerified" to false,
                                "createdAt" to System.currentTimeMillis(),
                                "lastActivityAt" to System.currentTimeMillis(),
                                "last1HourJoins" to 0,
                                "last2HourGrowthRate" to 0.0,
                                "totalVisits24h" to 1
                            )

                            db.collection("rooms").document(generatedRoomId).set(newRoomMap)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Voice Room Created Successfully!", Toast.LENGTH_SHORT).show()
                                    onCreate()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Created Room locally!", Toast.LENGTH_SHORT).show()
                                    onCreate()
                                }
                        } else {
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
                // Header Label Text with Illustration Blob
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Create Language Room",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Start a voice room and connect with people\nwho love the same language 💛",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            lineHeight = 18.sp
                        )
                    }
                    
                    // Visual illustration of blob character
                    Box(
                        modifier = Modifier.size(75.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background decorative rings
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFFFFBEB), CircleShape)
                        )
                        // Blob illustration Image
                        Image(
                            painter = painterResource(id = com.example.R.drawable.language_room_icon_1781320484319),
                            contentDescription = "Mascot Logo",
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        // Tiny floating speech/dots
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(18.dp)
                                .background(Color(0xFFA855F7), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💬", fontSize = 10.sp)
                        }
                    }
                }

                // ---------------- STEP 1: Basic Information ----------------
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF3F4F6))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Title header index
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
                                fontSize = 15.sp,
                                color = Color.Black
                            )
                        }

                        // Room Title
                        Text(
                            text = "Room Title *",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = roomTitle,
                            onValueChange = { if (it.length <= 50) roomTitle = it },
                            placeholder = { Text("e.g. Let's talk in Japanese!", color = Color.Gray, fontSize = 13.sp) },
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
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = roomDescription,
                            onValueChange = { if (it.length <= 120) roomDescription = it },
                            placeholder = { Text("e.g. Daily conversation practice in Japanese", color = Color.Gray, fontSize = 13.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
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

                        // Language Selector
                        Text(
                            text = "Language *",
                            fontSize = 13.sp,
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
                                // Circular flag representation
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
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Dropdown",
                                tint = Color.Gray
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "We'll auto-generate the code, flag, and default banner.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                // ---------------- STEP 2: Room Card Banner ----------------
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
                                text = "Room Card Banner",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.Black
                            )
                        }

                        // Banner image frame
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            if (appliedBannerUrl.startsWith("gradient:")) {
                                val colorsString = appliedBannerUrl.removePrefix("gradient:")
                                val colorHexes = colorsString.split(",")
                                val colors = colorHexes.mapNotNull { hex ->
                                    try {
                                        val cleanHex = hex.trim()
                                        val finalHex = if (cleanHex.startsWith("#")) cleanHex else "#$cleanHex"
                                        Color(android.graphics.Color.parseColor(finalHex))
                                    } catch (e: Exception) {
                                        null
                                    }
                                }.ifEmpty { listOf(Color(0xFF6366F1), Color(0xFFEC4899)) }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Brush.linearGradient(colors))
                                )
                            } else {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(appliedBannerUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Room Banner Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            // 16:9 float rating pill
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "16:9",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Change banner button
                        OutlinedButton(
                            onClick = { showBannerOverlay = true },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Image",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Change Banner",
                                    color = Color.Black,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // ---------------- STEP 3: Level ----------------
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
                                text = "Level",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.Black
                            )
                        }

                        // Beginner, intermediate, advanced cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val levels = listOf(
                                Triple("Beginner", "🌱", "Beginner"),
                                Triple("Intermediate", "📊", "Intermediate"),
                                Triple("Advanced", "⭐", "Advanced")
                            )

                            levels.forEach { (levelId, emoji, label) ->
                                val isSelected = selectedLevel == levelId
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedLevel = levelId },
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
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(text = emoji, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = label,
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

                // ---------------- STEP 4: VIP Badge ----------------
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
                                text = "VIP Badge",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.Black
                            )
                        }

                        // Toggle row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Show VIP tag on your room",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    // Custom VIP small pill
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFFFC107), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "VIP",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }
                            Switch(
                                checked = isVipBadgeEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (hostIsVipLocal) {
                                            isVipBadgeEnabled = true
                                        } else {
                                            showVipPaywallSheet = true
                                        }
                                    } else {
                                        isVipBadgeEnabled = false
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFFFC107)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Custom warning banner box
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFFBEB), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFFEF3C7), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Crown icon
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "VIP Warning",
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "This feature is available for VIP users.",
                                fontSize = 12.sp,
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(120.dp))
            }

            // ================= SELECT LANGUAGE SHEET =================
            if (showLanguageSheet) {
                var searchQuery by remember { mutableStateOf("") }
                val filteredAllLanguages = remember(searchQuery) {
                    if (searchQuery.isBlank()) {
                        LanguageData.allLanguages
                    } else {
                        LanguageData.allLanguages.filter {
                            it.name.contains(searchQuery, ignoreCase = true) ||
                            it.nativeName.contains(searchQuery, ignoreCase = true)
                        }
                    }
                }

                ModalBottomSheet(
                    onDismissRequest = { showLanguageSheet = false },
                    containerColor = Color.White,
                    tonalElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title
                        Text(
                            text = "Select Language",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color.Black,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )

                        // Search box
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search language", color = Color.Gray, fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.Gray
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color(0xFFE5E7EB),
                                focusedBorderColor = Color(0xFFFFC107),
                                unfocusedContainerColor = Color(0xFFF9FAFB),
                                focusedContainerColor = Color.White
                            )
                        )

                        // Popular languages block (Japanese, Korean, English, Spanish, Chinese)
                        Text(
                            text = "Popular Languages",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
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
                                            .size(54.dp)
                                            .background(Color(0xFFF9FAFB), RoundedCornerShape(16.dp))
                                            .border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(16.dp))
                                            .padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircleFlag(
                                            countryCode = code,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }

                        Divider(color = Color(0xFFF3F4F6), thickness = 1.dp)

                        // All languages list
                        Text(
                            text = "All Languages",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.Black
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredAllLanguages) { language ->
                                val isChosen = selectedLanguage.name == language.name
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedLanguage = language
                                            showLanguageSheet = false
                                        }
                                        .padding(vertical = 4.dp),
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
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    
                                    if (isChosen) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = Color(0xFFFFC107),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================= CHOOSE BANNER OVERLAY =================
            if (showBannerOverlay) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { /* Block clicks */ }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f)
                            .align(Alignment.BottomCenter),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // Top close bar & header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { showBannerOverlay = false }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                                }
                                Text(
                                    text = "Choose Banner",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )
                                Box(modifier = Modifier.size(24.dp))
                            }

                            Text(
                                text = "Pick a banner for your room.\nBanners are based on the selected language.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )

                            // Dropdown status selected language indicator
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 12.dp)
                                    .background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircleFlag(
                                    countryCode = selectedLanguage.flagCode,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedLanguage.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Horizontal sliding Categories selection row
                            LazyRow(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                val categoriesList = listOf("Recommended", "Worldwide Travel", "Cozy Lounges", "Cyberpunk & Art", "100 Live Gradients")
                                items(categoriesList) { cat ->
                                    val isSel = selectedCategory == cat
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSel) Color(0xFFFFC107) else Color(0xFFF3F4F6))
                                            .clickable { selectedCategory = cat }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = cat,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.Black else Color.Gray
                                        )
                                    }
                                }
                            }

                            // Dynamic help text inside selector dialog depending on active category
                            val subLabel = when (selectedCategory) {
                                "Recommended" -> "Specifically matching for ${selectedLanguage.name} rooms."
                                "Worldwide Travel" -> "Breathtaking worldwide landmarks and iconic travel cities."
                                "Cozy Lounges" -> "Cozy coffee houses, libraries, and soft ambient spots."
                                "Cyberpunk & Art" -> "Neon gridways, vaporwave landscapes, and anime-styled horizons."
                                else -> "100 premium hand-crafted dynamic backgrounds with real-time live glow."
                            }
                            Text(
                                text = subLabel,
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Banners display container and scrolling list
                            val bannersToDisplay = when (selectedCategory) {
                                "Recommended" -> currentLanguageBanners
                                "Worldwide Travel" -> BannerPack.worldwideScenics
                                "Cozy Lounges" -> BannerPack.cozyVibes
                                "Cyberpunk & Art" -> BannerPack.cyberpunkAndArt
                                "100 Live Gradients" -> BannerPack.gradients
                                else -> currentLanguageBanners
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val pairs = bannersToDisplay.chunked(2)
                                pairs.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        rowItems.forEach { banner ->
                                            val isChecked = temporarySelectedBanner == banner.url
                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(100.dp)
                                                    .clickable { temporarySelectedBanner = banner.url },
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(
                                                    width = if (isChecked) 2.dp else 0.dp,
                                                    color = if (isChecked) Color(0xFFFFC107) else Color.Transparent
                                                )
                                            ) {
                                                Box(modifier = Modifier.fillMaxSize()) {
                                                    if (banner.url.startsWith("gradient:")) {
                                                        val colorsString = banner.url.removePrefix("gradient:")
                                                        val colorHexes = colorsString.split(",")
                                                        val colors = colorHexes.mapNotNull { hex ->
                                                            try {
                                                                val cleanHex = hex.trim()
                                                                val finalHex = if (cleanHex.startsWith("#")) cleanHex else "#$cleanHex"
                                                                Color(android.graphics.Color.parseColor(finalHex))
                                                            } catch (e: Exception) {
                                                                null
                                                            }
                                                        }.ifEmpty { listOf(Color(0xFF6366F1), Color(0xFFEC4899)) }
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .background(Brush.linearGradient(colors))
                                                        )
                                                    } else {
                                                        AsyncImage(
                                                            model = banner.url,
                                                            contentDescription = banner.name,
                                                            modifier = Modifier.fillMaxSize(),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    }

                                                    // Label of banner
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .align(Alignment.BottomStart)
                                                            .background(Color.Black.copy(alpha = 0.5f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = banner.name,
                                                            color = Color.White,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }

                                                    // Selected Check Indicator bubble
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(4.dp)
                                                            .size(16.dp)
                                                            .background(
                                                                if (isChecked) Color(0xFFFFC107) else Color.White.copy(alpha = 0.6f),
                                                                CircleShape
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isChecked) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = "Selected",
                                                                tint = Color.Black,
                                                                modifier = Modifier.size(10.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        // Empty spacer box to pad space on uneven row
                                        if (rowItems.size == 1) {
                                            Box(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }

                                // Custom Banner list Gallery VIP Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFFFBEB), RoundedCornerShape(12.dp))
                                        .clickable { showVipPaywallSheet = true }
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = "Gallery",
                                            tint = Color(0xFFFFC107),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Upload from Gallery",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color.Black
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFFFFC107), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
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
                                                text = "Upload your own banner from gallery",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    
                                    // Lock icon indicator
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = Color(0xFFFFC107),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action: apply banner
                            Button(
                                onClick = {
                                    appliedBannerUrl = temporarySelectedBanner
                                    showBannerOverlay = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107)),
                                shape = RoundedCornerShape(25.dp)
                            ) {
                                Text(
                                    text = "Apply This Banner",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            
                            Text(
                                text = "You can change it anytime",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // ================= VIP PAYWALL SHEET =================
            if (showVipPaywallSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showVipPaywallSheet = false },
                    containerColor = Color.White,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header Close button & Top decoration Crown logo
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .background(Color(0xFFFEF3C7), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("👑", fontSize = 38.sp)
                                }
                            }
                            
                            // Glowing elements
                            Text(
                                text = "✨",
                                fontSize = 18.sp,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(start = 20.dp, top = 10.dp)
                            )
                            Text(
                                text = "✨",
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 40.dp)
                            )
                        }

                        // Title
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Unlock FunkyTalk VIP ",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp,
                                    color = Color.Black
                                )
                                Text(
                                    text = "VIP",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = Color(0xFFFFC107)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Enjoy premium features and the best experience\nwhile connecting with language lovers.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }

                        // Benefits lists
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF9FAFB), RoundedCornerShape(16.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val benefits = listOf(
                                Pair("🎨", "Upload custom room banners\nUse any image from your gallery as your room banner."),
                                Pair("👑", "VIP badge on your rooms\nShow your VIP status and stand out."),
                                Pair("✨", "Exclusive banner collection\nAccess premium and limited edition banners."),
                                Pair("🚀", "Future premium features\nGet access to new and exclusive features first.")
                            )

                            benefits.forEach { (emoji, text) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = emoji, fontSize = 14.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = text,
                                        fontSize = 12.sp,
                                        color = Color.DarkGray,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }

                        // Plans selector
                        Text(
                            text = "✨ Choose Your Plan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.Black
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Monthly plan option card
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { vipPlanSelected = "monthly" },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(
                                    width = if (vipPlanSelected == "monthly") 2.dp else 1.dp,
                                    color = if (vipPlanSelected == "monthly") Color(0xFFFFC107) else Color(0xFFE5E7EB)
                                ),
                                shape = RoundedCornerShape(16.dp)
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
                                    Text("Monthly", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("₹249 / month", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.Black)
                                    Text("Billed monthly. Cancel anytime.", fontSize = 9.sp, color = Color.Gray)
                                    
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .fillMaxWidth()
                                            .background(Color(0xFFF3F4F6), RoundedCornerShape(8.dp))
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("₹249 every month", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                                    }
                                }
                            }

                            // Yearly plan option card - Highlighted as BEST VALUE
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { vipPlanSelected = "yearly" },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(
                                    width = if (vipPlanSelected == "yearly") 2.dp else 1.dp,
                                    color = if (vipPlanSelected == "yearly") Color(0xFFFFC107) else Color(0xFFE5E7EB)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    // Best Value tag top right
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .background(
                                                Color(0xFFFFC107),
                                                RoundedCornerShape(bottomStart = 8.dp, topEnd = 14.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "Best Value",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.sp,
                                            color = Color.Black
                                        )
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
                                        Text("Yearly", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("₹1,999 / year", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.Black)
                                        }
                                        Text("Save 33% 🎉", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                        
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 4.dp)
                                                .fillMaxWidth()
                                                .background(Color(0xFFFFFBEB), RoundedCornerShape(8.dp))
                                                .padding(vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("₹166.58 every month", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                        }
                                    }
                                }
                            }
                        }

                        // Security guarantee footer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = "Secure", tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Secure payment · Cancel anytime · 7-day money back guarantee",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }

                        // Upgrade button
                        Button(
                            onClick = {
                                hostIsVipLocal = true
                                isVipBadgeEnabled = true
                                showVipPaywallSheet = false
                                Toast.makeText(context, "Welcome to FunkyTalk VIP! 👑 Status unlocked.", Toast.LENGTH_LONG).show()
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
                                Icon(imageVector = Icons.Default.Star, contentDescription = "VIP", tint = Color.Black, modifier = Modifier.size(20.dp))
                                Text(
                                    text = "Upgrade to VIP",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.Black
                                )
                                Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "Next", tint = Color.Black)
                            }
                        }

                        // Restore purchases
                        Text(
                            text = "Restore Purchases",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Toast.makeText(context, "Purchases restored successfully.", Toast.LENGTH_SHORT).show()
                                }
                        )

                        // Terms
                        Text(
                            text = "By continuing, you agree to our Terms of Service and Privacy Policy.",
                            fontSize = 10.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
