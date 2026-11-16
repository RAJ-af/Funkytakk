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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.CircleFlag
import com.example.ui.components.LocationConsentDialog
import com.example.ui.theme.LocalFunkyColors
import com.example.util.LocationDetector
import kotlinx.coroutines.delay
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

    // Async API-driven city lists (CountriesNow + Open-Meteo Geocoding)
    var isLoadingCountryCities by remember { mutableStateOf(false) }
    var isSearchingOnline by remember { mutableStateOf(false) }
    var apiCountryCities by remember {
        mutableStateOf(PlaceDbService.getCitiesForCountry(currentCountryCode))
    }
    var apiPopularCities by remember {
        mutableStateOf(PlaceDbService.getPopularCities(currentCountryCode))
    }
    var apiSearchResults by remember { mutableStateOf<List<PlaceSettlement>>(emptyList()) }

    // Fetch all cities for the active country whenever country changes
    LaunchedEffect(currentCountryCode, currentCountryName) {
        apiCountryCities = PlaceDbService.getCitiesForCountry(currentCountryCode)
        apiPopularCities = PlaceDbService.getPopularCities(currentCountryCode)
        isLoadingCountryCities = true
        val fetchedAll = PlaceDbService.fetchCitiesForCountry(currentCountryCode, currentCountryName)
        val fetchedPopular = PlaceDbService.fetchPopularCities(currentCountryCode, currentCountryName)
        apiCountryCities = fetchedAll
        apiPopularCities = fetchedPopular
        isLoadingCountryCities = false
    }

    // Debounced online fuzzy search when typing in search bar
    LaunchedEffect(searchQuery, currentCountryCode, currentCountryName) {
        val trimmed = searchQuery.trim()
        if (trimmed.isBlank()) {
            apiSearchResults = emptyList()
            isSearchingOnline = false
            return@LaunchedEffect
        }
        // Instant local/cached filter first so UI feels responsive
        apiSearchResults = PlaceDbService.searchSettlements(trimmed, currentCountryCode)
        isSearchingOnline = true
        delay(280L)
        val online = PlaceDbService.searchCitiesOnline(
            query = trimmed,
            countryCode = currentCountryCode,
            countryName = currentCountryName
        )
        apiSearchResults = online
        isSearchingOnline = false
    }

    val displayedSettlements = remember(searchQuery, apiCountryCities, apiSearchResults) {
        if (searchQuery.isBlank()) apiCountryCities else apiSearchResults
    }

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
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header: Title + subtitle, Country dropdown pill, Done CTA
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                    Text(
                        text = "Choose your city",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = funkyColors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Live cities in $currentCountryName",
                        fontSize = 12.sp,
                        color = funkyColors.mute,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Country Pill (Clickable to switch country)
                    Row(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(funkyColors.soft)
                            .border(1.dp, funkyColors.line, RoundedCornerShape(20.dp))
                            .testTag("city_country_picker_pill")
                            .clickable { onCountryClick() }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
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
                            fontWeight = FontWeight.SemiBold,
                            color = funkyColors.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 92.dp)
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
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        elevation = null,
                        modifier = Modifier
                            .height(40.dp)
                            .testTag("city_done_button")
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

            // Polished Search Bar with clear button & inline progress spinner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                color = funkyColors.bg,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (searchQuery.isNotEmpty()) funkyColors.mango else funkyColors.line
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 14.dp, end = 6.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = "Search",
                        tint = if (searchQuery.isNotEmpty()) funkyColors.mango else funkyColors.mute,
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
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(funkyColors.mango),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("city_search_input"),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search any city in $currentCountryName...",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        color = funkyColors.mute
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (isSearchingOnline || isLoadingCountryCities) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .size(16.dp),
                            color = funkyColors.mango,
                            strokeWidth = 2.dp
                        )
                    }

                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("city_search_clear_button")
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Clear search",
                                tint = funkyColors.mute,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 240.dp, max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Use My Current Location Card
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(funkyColors.mango.copy(alpha = if (funkyColors.isDark) 0.14f else 0.16f))
                            .border(1.dp, funkyColors.mango.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                            .testTag("city_use_current_location_card")
                            .clickable { triggerLocationDetection() }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(funkyColors.mango.copy(alpha = 0.25f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isDetectingLocation) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = funkyColors.mango,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_target),
                                        contentDescription = "Detect location",
                                        tint = funkyColors.mango,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isDetectingLocation) "Detecting location..." else "Use my current location",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
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
                            contentDescription = "Detect",
                            tint = funkyColors.mute,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 2. Popular Cities Section (when not searching)
                if (searchQuery.isBlank() && apiPopularCities.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "POPULAR CITIES",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.mute,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(apiPopularCities, key = { "pop_${it.countryCode}_${it.name}" }) { city ->
                                val isSelected = selectedCityName.equals(city.name, ignoreCase = true)
                                Row(
                                    modifier = Modifier
                                        .defaultMinSize(minHeight = 42.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSelected) funkyColors.mango.copy(alpha = 0.2f)
                                            else funkyColors.soft
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) funkyColors.mango else funkyColors.line,
                                            shape = RoundedCornerShape(14.dp)
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

                // 3. All Cities / Search Results Header
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "ALL CITIES" else "SEARCH RESULTS",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = funkyColors.mute,
                            letterSpacing = 0.8.sp
                        )
                        if (displayedSettlements.isNotEmpty()) {
                            Text(
                                text = "${displayedSettlements.size} found",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = funkyColors.mute
                            )
                        }
                    }
                }

                // Loading indicator when country cities are fetching cleanly
                if (searchQuery.isBlank() && isLoadingCountryCities && displayedSettlements.isEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(funkyColors.bg)
                                .padding(horizontal = 16.dp, vertical = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = funkyColors.mango,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Loading cities for $currentCountryName...",
                                fontSize = 13.sp,
                                color = funkyColors.mute
                            )
                        }
                    }
                }

                // Helpful empty state + one-tap custom selection when search has no API matches
                if (searchQuery.isNotBlank() && !isSearchingOnline && displayedSettlements.isEmpty()) {
                    val trimmed = searchQuery.trim()
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(funkyColors.bg)
                                .border(1.dp, funkyColors.line, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No matching city found for \"$trimmed\" in $currentCountryName.",
                                fontSize = 13.sp,
                                color = funkyColors.mute
                            )
                            Surface(
                                onClick = {
                                    selectedCityName = trimmed
                                    onCitySelected(trimmed)
                                    onDismiss()
                                },
                                color = funkyColors.mango.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, funkyColors.mango)
                            ) {
                                Text(
                                    text = "Use \"$trimmed\"",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = funkyColors.ink,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                )
                            }
                        }
                    }
                }

                // 4. Cities List
                items(
                    items = displayedSettlements,
                    key = { "${it.countryCode}_${it.name}_${it.adminRegion}" }
                ) { settlement ->
                    val isSelected = selectedCityName.equals(settlement.name, ignoreCase = true)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) funkyColors.mango.copy(alpha = 0.16f)
                                else funkyColors.bg.copy(alpha = 0.55f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) funkyColors.mango.copy(alpha = 0.55f) else Color.Transparent,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                selectedCityName = settlement.name
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                    .background(
                                        if (isSelected) funkyColors.mango.copy(alpha = 0.22f) else funkyColors.soft,
                                        CircleShape
                                    ),
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
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = funkyColors.ink
                                )
                                val subRegion = buildString {
                                    if (settlement.adminRegion.isNotBlank() &&
                                        !settlement.adminRegion.equals(settlement.countryName, ignoreCase = true)
                                    ) {
                                        append(settlement.adminRegion)
                                    }
                                    if (settlement.countryName.isNotBlank()) {
                                        if (isNotEmpty()) append(", ")
                                        append(settlement.countryName)
                                    }
                                }
                                if (subRegion.isNotBlank()) {
                                    Text(
                                        text = subRegion,
                                        fontSize = 12.sp,
                                        color = funkyColors.mute
                                    )
                                }
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
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(funkyColors.bg)
                            .border(1.dp, funkyColors.line, RoundedCornerShape(14.dp))
                            .testTag("city_manual_entry_card")
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
                        placeholder = { Text("e.g. Lahore, Rawalpindi", color = funkyColors.mute) },
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
