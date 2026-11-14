package com.example.ui.screens.profile.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.CircleFlag
import com.example.ui.components.LocationConsentDialog
import com.example.ui.theme.LocalFunkyColors
import com.example.util.LocationDetector
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChooseCitySheet(
    currentCity: String,
    currentCountryName: String,
    currentCountryCode: String,
    onCountryClick: () -> Unit,
    onCitySelected: (cityName: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val funkyColors = LocalFunkyColors.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedCityName by remember { mutableStateOf(currentCity) }
    var detectedCitySubtext by remember { mutableStateOf("Tap to detect device location") }
    var isDetectingLocation by remember { mutableStateOf(false) }
    var showConsentDialog by remember { mutableStateOf(false) }

    // Dialog state for manual city input
    var showManualInputDialog by remember { mutableStateOf(false) }
    var manualCityInput by remember { mutableStateOf("") }

    // Location Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (fineGranted || coarseGranted) {
            if (!LocationDetector.isGpsEnabled(context)) {
                Toast.makeText(context, "Location services (GPS) disabled on device", Toast.LENGTH_SHORT).show()
                detectedCitySubtext = "GPS disabled in settings"
                return@rememberLauncherForActivityResult
            }

            coroutineScope.launch {
                isDetectingLocation = true
                detectedCitySubtext = "Detecting GPS location..."
                val detected = LocationDetector.detectCurrentLocation(context)
                isDetectingLocation = false

                if (detected != null && detected.city.isNotBlank()) {
                    selectedCityName = detected.city
                    detectedCitySubtext = "${detected.city}, ${detected.country}"
                    onCitySelected(detected.city)
                    Toast.makeText(context, "Detected: ${detected.city}, ${detected.country}", Toast.LENGTH_SHORT).show()
                    onDismiss()
                } else {
                    detectedCitySubtext = "Could not resolve GPS location"
                    Toast.makeText(context, "Could not resolve location. Please select city manually.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            detectedCitySubtext = "Permission denied"
            Toast.makeText(context, "Location permission denied. Please select city manually.", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerLocationDetection() {
        if (!LocationDetector.hasLocationPermission(context)) {
            showConsentDialog = true
        } else {
            if (!LocationDetector.isGpsEnabled(context)) {
                Toast.makeText(context, "Location services (GPS) disabled on device", Toast.LENGTH_SHORT).show()
                return
            }

            coroutineScope.launch {
                isDetectingLocation = true
                detectedCitySubtext = "Detecting GPS location..."
                val detected = LocationDetector.detectCurrentLocation(context)
                isDetectingLocation = false

                if (detected != null && detected.city.isNotBlank()) {
                    selectedCityName = detected.city
                    detectedCitySubtext = "${detected.city}, ${detected.country}"
                    onCitySelected(detected.city)
                    Toast.makeText(context, "Detected: ${detected.city}, ${detected.country}", Toast.LENGTH_SHORT).show()
                    onDismiss()
                } else {
                    detectedCitySubtext = "Could not resolve GPS location"
                    Toast.makeText(context, "Could not resolve location. Please select city manually.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Popular cities filtered strictly for active country
    val popularCities = remember(currentCountryCode) {
        PlaceDbService.getPopularCities(currentCountryCode)
    }

    // Filtered/search settlements
    val settlementsList = remember(searchQuery, currentCountryCode) {
        PlaceDbService.searchSettlements(searchQuery, currentCountryCode)
    }

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
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header: Title, Country dropdown pill, Done button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Choose your city",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = funkyColors.ink
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Country Pill (Clickable to switch country)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(funkyColors.soft)
                            .border(1.dp, funkyColors.line, RoundedCornerShape(20.dp))
                            .clickable { onCountryClick() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                        ) {
                            CircleFlag(
                                countryCode = currentCountryCode,
                                modifier = Modifier.fillMaxSize(),
                                allowProgramming = false
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentCountryName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = funkyColors.ink
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chevron_down),
                            contentDescription = "Change country",
                            tint = funkyColors.mute,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Done Button
                    Button(
                        onClick = {
                            if (selectedCityName.isNotBlank()) {
                                onCitySelected(selectedCityName)
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = funkyColors.mango,
                            contentColor = funkyColors.onMango
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                        elevation = null,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "Done",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = funkyColors.onMango
                        )
                    }
                }
            }

            // Search Bar: Search cities in <Country>...
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(funkyColors.bg, RoundedCornerShape(14.dp))
                    .border(1.dp, funkyColors.line, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = "Search",
                        tint = funkyColors.mute,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            color = funkyColors.ink,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = SolidColor(funkyColors.mango),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search cities in $currentCountryName...",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        color = funkyColors.mute
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Use My Current Location Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(funkyColors.mango.copy(alpha = if (funkyColors.isDark) 0.14f else 0.18f))
                            .border(1.dp, funkyColors.mango.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                            .clickable {
                                triggerLocationDetection()
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(funkyColors.mango.copy(alpha = 0.25f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_pin),
                                    contentDescription = "Location",
                                    tint = funkyColors.mango,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isDetectingLocation) "Detecting location..." else "Use my current location",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = funkyColors.ink
                                )
                                Text(
                                    text = detectedCitySubtext,
                                    fontSize = 12.sp,
                                    color = funkyColors.mute
                                )
                            }
                        }

                        Icon(
                            painter = painterResource(id = R.drawable.ic_chevron_right),
                            contentDescription = "Go",
                            tint = funkyColors.mute,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 2. Popular Cities Section
                if (searchQuery.isBlank() && popularCities.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "POPULAR CITIES",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.mute,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(popularCities) { city ->
                                val isSelected = selectedCityName.equals(city.name, ignoreCase = true)
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) funkyColors.mango.copy(alpha = 0.18f)
                                            else funkyColors.soft
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) funkyColors.mango else funkyColors.line,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            selectedCityName = city.name
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_pin),
                                        contentDescription = null,
                                        tint = if (isSelected) funkyColors.mango else funkyColors.mute,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = city.name,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = funkyColors.ink
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. All Cities Header
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "ALL CITIES" else "SEARCH RESULTS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = funkyColors.mute,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // 4. Cities List
                items(settlementsList) { settlement ->
                    val isSelected = selectedCityName.equals(settlement.name, ignoreCase = true)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) funkyColors.mango.copy(alpha = 0.16f)
                                else Color.Transparent
                            )
                            .clickable {
                                selectedCityName = settlement.name
                            }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(funkyColors.soft, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_pin),
                                    contentDescription = null,
                                    tint = if (isSelected) funkyColors.mango else funkyColors.mute,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = settlement.name,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = funkyColors.ink
                                )
                                Text(
                                    text = if (settlement.countryName != currentCountryName) {
                                        "${settlement.adminRegion}, ${settlement.countryName}"
                                    } else {
                                        settlement.adminRegion
                                    },
                                    fontSize = 12.sp,
                                    color = funkyColors.mute
                                )
                            }
                        }

                        // Radio checkmark indicator
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(funkyColors.mango, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check),
                                    contentDescription = "Selected",
                                    tint = funkyColors.onMango,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .border(1.5.dp, funkyColors.line, CircleShape)
                            )
                        }
                    }
                }

                // 5. City Not Listed? Enter It Manually Row
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(funkyColors.bg)
                            .border(1.dp, funkyColors.line, RoundedCornerShape(14.dp))
                            .clickable {
                                manualCityInput = ""
                                showManualInputDialog = true
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(funkyColors.soft, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_fix),
                                    contentDescription = "Enter manually",
                                    tint = funkyColors.ink,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "City not listed?",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = funkyColors.ink
                                )
                                Text(
                                    text = "Enter it manually",
                                    fontSize = 12.sp,
                                    color = funkyColors.mute
                                )
                            }
                        }

                        Icon(
                            painter = painterResource(id = R.drawable.ic_chevron_right),
                            contentDescription = "Open",
                            tint = funkyColors.mute,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // Manual City Input Modal Dialog
    if (showManualInputDialog) {
        AlertDialog(
            onDismissRequest = { showManualInputDialog = false },
            containerColor = funkyColors.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Enter Custom City",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = funkyColors.ink
                )
            },
            text = {
                Column {
                    Text(
                        text = "Enter the name of your city or town in $currentCountryName:",
                        fontSize = 13.sp,
                        color = funkyColors.mute,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = manualCityInput,
                        onValueChange = { manualCityInput = it },
                        placeholder = { Text("e.g. Noida, Gurgaon", color = funkyColors.mute) },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp, color = funkyColors.ink),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = funkyColors.mango,
                            unfocusedBorderColor = funkyColors.line,
                            focusedContainerColor = funkyColors.bg,
                            unfocusedContainerColor = funkyColors.bg,
                            focusedTextColor = funkyColors.ink,
                            unfocusedTextColor = funkyColors.ink,
                            cursorColor = funkyColors.mango
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = manualCityInput.trim()
                        if (trimmed.isNotBlank()) {
                            selectedCityName = trimmed
                            onCitySelected(trimmed)
                            showManualInputDialog = false
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Please enter a city name", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = funkyColors.mango,
                        contentColor = funkyColors.onMango
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save City", fontWeight = FontWeight.Bold, color = funkyColors.onMango)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualInputDialog = false }) {
                    Text("Cancel", color = funkyColors.mute)
                }
            }
        )
    }

    if (showConsentDialog) {
        LocationConsentDialog(
            onDismiss = { showConsentDialog = false },
            onConfirmConsent = {
                showConsentDialog = false
                locationPermissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        )
    }
}
