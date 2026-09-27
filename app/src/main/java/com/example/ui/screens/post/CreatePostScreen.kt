package com.example.ui.screens.post

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.BuildConfig
import com.example.SupabaseClient
import com.example.SupabaseSessionManager
import com.example.CloudinaryUploadResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreen(
    onDismiss: () -> Unit,
    onPostCreated: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val selfUserId = remember(context) { SupabaseSessionManager.getUserId(context) ?: "" }

    // "Photo", "Video", "Text" (Only Text) post types
    var selectedTab by remember { mutableStateOf("Photo") }

    // State of selected media
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedPresetIndex by remember { mutableStateOf<Int?>(null) }
    var showPresetSelector by remember { mutableStateOf(false) }

    // App variables
    var captionText by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("Kuala Lumpur, Malaysia") }
    var isDetectingLocation by remember { mutableStateOf(false) }

    // Multi-select for optional lists
    val selectedLanguages = remember { mutableStateListOf("Malay", "English") }
    val selectedTopics = remember { mutableStateListOf("Travel", "Nature") }
    var showAddLanguagePanel by remember { mutableStateOf(false) }

    // Publishing state
    var isUploading by remember { mutableStateOf(false) }
    var showSuccessAnimation by remember { mutableStateOf(false) }

    // Check & Manage Media Permission State
    var hasMediaPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    // Media preset lists (high-quality Unsplash URLs)
    val presetImages = listOf(
        "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=600", // Sunset city
        "https://images.unsplash.com/photo-1513836279014-a89f7a76ae86?w=600", // Forest pathway
        "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=600", // Cat sleeping
        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=600"  // Sunset beach
    )

    val presetVideos = listOf(
        "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=600", // Movie reel
        "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?w=600", // Nature camera
        "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=600"  // Cinema director
    )

    // Pre-populate with first preset on launch to ensure a premium mockup look right away
    LaunchedEffect(selectedTab) {
        if (selectedTab != "Text" && selectedImageUri == null && selectedPresetIndex == null) {
            selectedPresetIndex = 0
        }
    }

    // Image & Video Pickers
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            selectedPresetIndex = null
            showPresetSelector = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] == true ||
            permissions[Manifest.permission.READ_MEDIA_VIDEO] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        hasMediaPermission = granted
        if (granted) {
            val requestType = if (selectedTab == "Video") {
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            } else {
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            }
            imagePickerLauncher.launch(requestType)
        } else {
            Toast.makeText(context, "Permission denied. Curated presets are available below.", Toast.LENGTH_SHORT).show()
            showPresetSelector = true
        }
    }

    fun requestMediaAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
            )
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    fun handlePostPublish() {
        if (captionText.isBlank() && selectedTab == "Text") {
            Toast.makeText(context, "Please write a caption first", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedTab != "Text" && selectedImageUri == null && selectedPresetIndex == null) {
            Toast.makeText(context, "Please select an image or video first", Toast.LENGTH_SHORT).show()
            return
        }

        isUploading = true
        coroutineScope.launch {
            try {
                val token = SupabaseSessionManager.getAuthenticatedBearerToken(context)
                    ?: SupabaseSessionManager.getBearerToken(context)
                    ?: "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"

                val secureUrls = mutableListOf<String>()

                // Upload custom media to Cloudinary if a device URI is selected
                if (selectedTab != "Text") {
                    val finalUri = if (selectedPresetIndex != null) {
                        Uri.parse(if (selectedTab == "Video") presetVideos[selectedPresetIndex!!] else presetImages[selectedPresetIndex!!])
                    } else {
                        selectedImageUri
                    }

                    if (finalUri != null) {
                        val uriStr = finalUri.toString()
                        if (uriStr.startsWith("http")) {
                            secureUrls.add(uriStr)
                        } else {
                            // Request upload signature from Supabase backend
                            val sigResp = withContext(Dispatchers.IO) {
                                SupabaseClient.api.getCloudinarySignature(
                                    apiKey = BuildConfig.SUPABASE_ANON_KEY,
                                    auth = token
                                )
                            }
                            if (sigResp.isSuccessful && sigResp.body() != null) {
                                val sigData = sigResp.body()!!
                                val inputStream = context.contentResolver.openInputStream(finalUri)
                                val fileBytes = inputStream?.readBytes()
                                inputStream?.close()

                                if (fileBytes != null) {
                                    val resourceType = if (selectedTab == "Video") "video" else "image"
                                    val mimeType = if (selectedTab == "Video") "video/mp4" else "image/jpeg"
                                    val uploadUrl = "https://api.cloudinary.com/v1_1/${sigData.cloud_name}/$resourceType/upload"

                                    val requestBody = MultipartBody.Builder()
                                        .setType(MultipartBody.FORM)
                                        .addFormDataPart("file", "file", RequestBody.create(mimeType.toMediaTypeOrNull(), fileBytes))
                                        .addFormDataPart("api_key", sigData.api_key)
                                        .addFormDataPart("timestamp", sigData.timestamp.toString())
                                        .addFormDataPart("signature", sigData.signature)
                                        .addFormDataPart("folder", sigData.folder)
                                        .build()

                                    val request = Request.Builder().url(uploadUrl).post(requestBody).build()
                                    val client = OkHttpClient()
                                    val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                                    val respBodyStr = response.body?.string()
                                    if (response.isSuccessful && respBodyStr != null) {
                                        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                                        val uploadResp = moshi.adapter(CloudinaryUploadResponse::class.java).fromJson(respBodyStr)
                                        uploadResp?.secure_url?.let { secureUrls.add(it) }
                                    }
                                }
                            }
                        }
                    }
                }

                // Format the post caption to combine text + selected metadata safely inside the "content" column,
                // avoiding DB exceptions from non-existent schema columns (e.g. location, languages)!
                val formattedContent = buildString {
                    append(captionText)
                    if (selectedLocation.isNotBlank() && selectedLocation != "Add location") {
                        append("\n\nLocation: ")
                        append(selectedLocation)
                    }
                    if (selectedLanguages.isNotEmpty()) {
                        append("\nLanguages: ")
                        append(selectedLanguages.joinToString(", "))
                    }
                    if (selectedTopics.isNotEmpty()) {
                        append("\nTopics: ")
                        append(selectedTopics.joinToString(" ") { "#$it" })
                    }
                }

                // Prepare backend parameters with standard moments table columns
                val momentBody = mapOf<String, Any?>(
                    "user_id" to selfUserId,
                    "content" to formattedContent,
                    "image_urls" to secureUrls
                )

                val postResp = withContext(Dispatchers.IO) {
                    SupabaseClient.api.createMoment(
                        apiKey = BuildConfig.SUPABASE_ANON_KEY,
                        auth = token,
                        moment = momentBody
                    )
                }

                if (postResp.isSuccessful) {
                    showSuccessAnimation = true
                } else {
                    Toast.makeText(context, "Failed to publish post", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("CreatePost", "Error publishing post", e)
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isUploading = false
            }
        }
    }

    if (showSuccessAnimation) {
        SuccessAnimationScreen(
            onComplete = {
                showSuccessAnimation = false
                onPostCreated()
            }
        )
    } else {
        // Redesigned premium FunkyTalk Create Post container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAF8F5)) // Premium warm off-white background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // 1. TOP HEADER (Compact, Custom Elements)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back arrow inside a subtle rounded circle button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFFECEBE6), CircleShape)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        CustomBackIcon(iconSize = 18.dp, color = Color(0xFF0F172A))
                    }

                    // Centered clean title
                    Text(
                        text = "Create Post",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )

                    // Post button (Yellow pill shape)
                    Surface(
                        onClick = { handlePostPublish() },
                        color = Color(0xFFFFD43B),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color(0xFF0F172A), strokeWidth = 2.dp)
                            } else {
                                Text(
                                    text = "Post",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }

                // Scrollable main screen contents
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(6.dp))

                    // GEN-Z POST TYPE SEGMENTED CHIPS SELECTOR (Photo / Video / Only Text)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1EFEA), RoundedCornerShape(24.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Photo", "Video", "Text").forEach { typeName ->
                            val isSelected = selectedTab == typeName
                            val tabLabel = if (typeName == "Text") {
                                "Only Text"
                            } else {
                                typeName
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) Color.White else Color.Transparent)
                                    .clickable {
                                        selectedTab = typeName
                                        if (typeName == "Text") {
                                            selectedPresetIndex = null
                                            selectedImageUri = null
                                        } else {
                                            selectedPresetIndex = 0
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabLabel,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isSelected) Color(0xFF0F172A) else Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 2. MEDIA SECTION (Compact, displayed for Photo/Video type only)
                    if (selectedTab != "Text") {
                        val selectedMediaModel = if (selectedPresetIndex != null) {
                            if (selectedTab == "Video") presetVideos[selectedPresetIndex!!] else presetImages[selectedPresetIndex!!]
                        } else {
                            selectedImageUri
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(94.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Large Rounded Media Preview Card (if loaded)
                            if (selectedMediaModel != null) {
                                Box(
                                    modifier = Modifier
                                        .size(94.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFFE2E8F0))
                                ) {
                                    AsyncImage(
                                        model = selectedMediaModel,
                                        contentDescription = "Selected media preview",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Display Custom Play overlay for Video
                                    if (selectedTab == "Video") {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CustomPlayArrowIcon(iconSize = 12.dp, color = Color.White)
                                        }
                                    }

                                    // Circular clean remove X button in top right
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.5f))
                                            .clickable {
                                                selectedPresetIndex = null
                                                selectedImageUri = null
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CustomCloseIcon(iconSize = 8.dp, color = Color.White)
                                    }
                                }
                            }

                            // Small "Add Photo/Video" tile beside it
                            Box(
                                modifier = Modifier
                                    .size(94.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .drawBehind {
                                        val stroke = Stroke(
                                            width = 1.5.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                                        )
                                        drawRoundRect(
                                            color = Color(0xFFDCDAD3),
                                            style = stroke,
                                            cornerRadius = CornerRadius(16.dp.toPx())
                                        )
                                    }
                                    .clickable {
                                        if (hasMediaPermission) {
                                            val requestType = if (selectedTab == "Video") {
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                            } else {
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            }
                                            imagePickerLauncher.launch(requestType)
                                        } else {
                                            requestMediaAccess()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CustomPlusIcon(iconSize = 16.dp, color = Color(0xFF64748B))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (selectedTab == "Video") "Add Video" else "Add Photo",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        // Presets Drawer (Collapsible list showing beautiful high-quality presets below it)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPresetSelector = !showPresetSelector }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Choose from curated presets",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFFB300)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showPresetSelector) "Hide" else "Show",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        AnimatedVisibility(visible = showPresetSelector) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val currentPresets = if (selectedTab == "Video") presetVideos else presetImages
                                currentPresets.forEachIndexed { index, presetUrl ->
                                    val isSelected = selectedPresetIndex == index
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 0.dp,
                                                color = if (isSelected) Color(0xFFFFD43B) else Color.Transparent,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedPresetIndex = index
                                                selectedImageUri = null
                                            }
                                    ) {
                                        AsyncImage(
                                            model = presetUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // 3. CAPTION SECTION (Clean, Rounded White Card)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFECEBE6), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Column {
                            TextField(
                                value = captionText,
                                onValueChange = { text -> if (text.length <= 500) captionText = text },
                                placeholder = {
                                    Text(
                                        text = "What’s on your mind? Share your language journey…",
                                        fontSize = 14.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp),
                                textStyle = TextStyle(
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A),
                                    lineHeight = 20.sp
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = Color(0xFF0F172A)
                                )
                            )

                            // Subtle Character Count indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    text = "${captionText.length}/500",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. LOCATION SECTION (Single clean Location Card)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFECEBE6), RoundedCornerShape(16.dp)),
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFF9FAF8), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CustomLocationPinIcon(iconSize = 18.dp, color = Color(0xFF0F172A))
                                }

                                Column {
                                    Text(
                                        text = "Add location",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF64748B)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = selectedLocation,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }

                            // Far right auto-detect location GPS action icon button
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFAF9F5))
                                    .border(1.dp, Color(0xFFECEBE6), CircleShape)
                                    .clickable {
                                        coroutineScope.launch {
                                            isDetectingLocation = true
                                            selectedLocation = "Detecting Location..."
                                            delay(1200)
                                            selectedLocation = "Kuala Lumpur, Malaysia"
                                            isDetectingLocation = false
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDetectingLocation) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color(0xFF0F172A), strokeWidth = 1.5.dp)
                                } else {
                                    CustomGpsIcon(iconSize = 14.dp, color = Color(0xFF0F172A))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 5. LANGUAGES SECTION
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Beautiful custom country/globe line icon next to Languages title!
                            CustomLanguageGlobeIcon(iconSize = 18.dp, color = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Languages",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Optional",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        // Add button (Plus symbol inside a clean round button)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, Color(0xFFECEBE6), CircleShape)
                                .clickable { showAddLanguagePanel = !showAddLanguagePanel },
                            contentAlignment = Alignment.Center
                        ) {
                            CustomPlusIcon(iconSize = 14.dp, color = Color(0xFF0F172A))
                        }
                    }

                    // Optional language panel expander
                    AnimatedVisibility(visible = showAddLanguagePanel) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Korean", "Spanish", "Japanese", "French", "German").forEach { lang ->
                                val alreadySelected = selectedLanguages.contains(lang)
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (alreadySelected) Color(0xFFFFD43B).copy(alpha = 0.3f) else Color.White,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .border(1.dp, Color(0xFFECEBE6), RoundedCornerShape(14.dp))
                                        .clickable {
                                            if (alreadySelected) selectedLanguages.remove(lang) else selectedLanguages.add(lang)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(text = lang, fontSize = 11.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Selected languages list as rounded chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        selectedLanguages.forEach { lang ->
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.border(1.dp, Color(0xFFECEBE6), RoundedCornerShape(20.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Language text
                                    Text(
                                        text = lang,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )

                                    // Remove X button
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF1F0EC))
                                            .clickable { selectedLanguages.remove(lang) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CustomCloseIcon(iconSize = 6.dp, color = Color(0xFF0F172A))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 6. TOPICS SECTION
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Add to topic",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Optional",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Naturally wrapping select chips using FlowRow
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val availableTopics = listOf(
                            "Travel", "Daily Life", "Food", "Study", "Nature", "City",
                            "Culture", "Music", "Movies", "Technology", "Sports"
                        )
                        availableTopics.forEach { topic ->
                            val isSelected = selectedTopics.contains(topic)
                            Surface(
                                color = if (isSelected) Color(0xFFFFD43B) else Color(0xFFECEAE4).copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .border(
                                        width = if (isSelected) 0.dp else 1.dp,
                                        color = Color(0xFFECEBE6),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        if (isSelected) selectedTopics.remove(topic) else selectedTopics.add(topic)
                                    }
                            ) {
                                Text(
                                    text = "#$topic",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(30.dp))

                    // 7. BOTTOM ACTION (Large Full-Width Button with Paper Plane icon)
                    Surface(
                        onClick = { handlePostPublish() },
                        color = Color(0xFFFFD43B),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF0F172A), strokeWidth = 2.5.dp)
                            } else {
                                CustomPaperPlaneIcon(iconSize = 18.dp, color = Color(0xFF0F172A))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Post",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// CURATED CANVAS LINE-DRAWING ICONS (2PX STROKE / PREMIUM NO-MATERIAL DESIGN)
// -----------------------------------------------------------------------------

@Composable
fun CustomBackIcon(modifier: Modifier = Modifier, iconSize: Dp = 18.dp, color: Color = Color(0xFF0F172A)) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.6f, h * 0.25f)
            lineTo(w * 0.35f, h * 0.5f)
            lineTo(w * 0.6f, h * 0.75f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawLine(
            color = color,
            start = Offset(w * 0.35f, h * 0.5f),
            end = Offset(w * 0.75f, h * 0.5f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun CustomCloseIcon(modifier: Modifier = Modifier, iconSize: Dp = 8.dp, color: Color = Color.White) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.75.dp.toPx()
        drawLine(color, Offset(w * 0.25f, h * 0.25f), Offset(w * 0.75f, h * 0.75f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.75f, h * 0.25f), Offset(w * 0.25f, h * 0.75f), strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
fun CustomPlusIcon(modifier: Modifier = Modifier, iconSize: Dp = 16.dp, color: Color = Color(0xFF0F172A)) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2.dp.toPx()
        drawLine(color, Offset(w * 0.5f, h * 0.22f), Offset(w * 0.5f, h * 0.78f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.22f, h * 0.5f), Offset(w * 0.78f, h * 0.5f), strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
fun CustomLocationPinIcon(modifier: Modifier = Modifier, iconSize: Dp = 18.dp, color: Color = Color(0xFF0F172A)) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            cubicTo(w * 0.24f, h * 0.15f, w * 0.24f, h * 0.5f, w * 0.5f, h * 0.85f)
            cubicTo(w * 0.76f, h * 0.5f, w * 0.76f, h * 0.15f, w * 0.5f, h * 0.15f)
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawCircle(
            color = color,
            radius = w * 0.12f,
            center = Offset(w * 0.5f, h * 0.4f),
            style = Stroke(width = strokeWidth)
        )
    }
}

@Composable
fun CustomGpsIcon(modifier: Modifier = Modifier, iconSize: Dp = 14.dp, color: Color = Color(0xFF0F172A)) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 1.75.dp.toPx()
        drawCircle(
            color = color,
            radius = w * 0.26f,
            center = center,
            style = Stroke(width = strokeWidth)
        )
        drawCircle(
            color = color,
            radius = w * 0.08f,
            center = center
        )
        // Cardboard ticks
        drawLine(color, Offset(w * 0.5f, h * 0.1f), Offset(w * 0.5f, h * 0.24f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.5f, h * 0.76f), Offset(w * 0.5f, h * 0.9f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.1f, h * 0.5f), Offset(w * 0.24f, h * 0.5f), strokeWidth, cap = StrokeCap.Round)
        drawLine(color, Offset(w * 0.76f, h * 0.5f), Offset(w * 0.9f, h * 0.5f), strokeWidth, cap = StrokeCap.Round)
    }
}

@Composable
fun CustomPaperPlaneIcon(modifier: Modifier = Modifier, iconSize: Dp = 18.dp, color: Color = Color(0xFF0F172A)) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.15f, h * 0.48f)
            lineTo(w * 0.85f, h * 0.15f)
            lineTo(w * 0.54f, h * 0.85f)
            lineTo(w * 0.44f, h * 0.56f)
            close()
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawLine(
            color = color,
            start = Offset(w * 0.44f, h * 0.56f),
            end = Offset(w * 0.85f, h * 0.15f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun CustomPlayArrowIcon(modifier: Modifier = Modifier, iconSize: Dp = 12.dp, color: Color = Color.White) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.35f, h * 0.25f)
            lineTo(w * 0.75f, h * 0.5f)
            lineTo(w * 0.35f, h * 0.75f)
            close()
        }
        drawPath(path = path, color = color)
    }
}

@Composable
fun CustomLanguageGlobeIcon(modifier: Modifier = Modifier, iconSize: Dp = 18.dp, color: Color = Color(0xFF0F172A)) {
    Canvas(modifier = modifier.size(iconSize)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2.dp.toPx()
        
        // Draw an outer circle for the globe representation
        drawCircle(
            color = color,
            radius = w * 0.4f,
            center = center,
            style = Stroke(width = strokeWidth)
        )
        // Draw equator horizontal line
        drawLine(
            color = color,
            start = Offset(w * 0.1f, h * 0.5f),
            end = Offset(w * 0.9f, h * 0.5f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Draw prime meridian vertical line
        drawLine(
            color = color,
            start = Offset(w * 0.5f, h * 0.1f),
            end = Offset(w * 0.5f, h * 0.9f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        // Draw latitude meridian curves
        val pathLeft = Path().apply {
            moveTo(w * 0.5f, h * 0.1f)
            quadraticTo(w * 0.23f, h * 0.5f, w * 0.5f, h * 0.9f)
        }
        val pathRight = Path().apply {
            moveTo(w * 0.5f, h * 0.1f)
            quadraticTo(w * 0.77f, h * 0.5f, w * 0.5f, h * 0.9f)
        }
        drawPath(pathLeft, color, style = Stroke(strokeWidth, cap = StrokeCap.Round))
        drawPath(pathRight, color, style = Stroke(strokeWidth, cap = StrokeCap.Round))
    }
}

// -----------------------------------------------------------------------------
// POST SUCCESS FULLSCREEN ANIMATION OVERLAY
// -----------------------------------------------------------------------------

@Composable
fun SuccessAnimationScreen(onComplete: () -> Unit) {
    val scale = remember { Animatable(0f) }
    val checkmarkPercent = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Step 1: Scale up the checkmark circle with a custom bouncy animation
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        // Step 2: Draw Checkmark path lines sequentially
        checkmarkPercent.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
        )
        // Step 3: Fade in completion words nicely
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 350)
        )
        delay(2200)
        onComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer(scaleX = scale.value, scaleY = scale.value)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Draw filled Soft Gold circle background
                    drawCircle(
                        color = Color(0xFFFFD43B),
                        radius = w * 0.44f,
                        center = center
                    )

                    // Draw thick black outline circle
                    drawCircle(
                        color = Color(0xFF0F172A),
                        radius = w * 0.44f,
                        center = center,
                        style = Stroke(width = w * 0.05f)
                    )

                    // Progressively draw checkmark based on checkmarkPercent
                    val fullPath = Path().apply {
                        moveTo(w * 0.33f, h * 0.51f)
                        lineTo(w * 0.46f, h * 0.63f)
                        lineTo(w * 0.67f, h * 0.39f)
                    }

                    drawPath(
                        path = fullPath,
                        color = Color(0xFF0F172A),
                        style = Stroke(
                            width = w * 0.07f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer(alpha = textAlpha.value)
            ) {
                Text(
                    text = "Post Successful!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Your moment has been shared with your followers.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
            }
        }
    }
}
