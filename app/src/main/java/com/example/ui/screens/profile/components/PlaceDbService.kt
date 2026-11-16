package com.example.ui.screens.profile.components

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class PlaceSettlement(
    val name: String,
    val adminRegion: String,
    val countryCode: String,
    val countryName: String,
    val isPopular: Boolean = false
)

object PlaceDbService {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    private val countryCitiesCache = ConcurrentHashMap<String, List<PlaceSettlement>>()
    private val searchCache = ConcurrentHashMap<String, List<PlaceSettlement>>()

    // Minimal offline seed fallback used only if network is unavailable
    private val offlineSeedSettlements: List<PlaceSettlement> = listOf(
        PlaceSettlement("New Delhi", "National Capital Territory", "in", "India", isPopular = true),
        PlaceSettlement("Mumbai", "Maharashtra", "in", "India", isPopular = true),
        PlaceSettlement("Bengaluru", "Karnataka", "in", "India", isPopular = true),
        PlaceSettlement("Hyderabad", "Telangana", "in", "India", isPopular = true),
        PlaceSettlement("Chennai", "Tamil Nadu", "in", "India", isPopular = true),
        PlaceSettlement("Kolkata", "West Bengal", "in", "India", isPopular = true),
        PlaceSettlement("New York", "New York", "us", "United States", isPopular = true),
        PlaceSettlement("Los Angeles", "California", "us", "United States", isPopular = true),
        PlaceSettlement("London", "Greater London", "gb", "United Kingdom", isPopular = true),
        PlaceSettlement("Toronto", "Ontario", "ca", "Canada", isPopular = true),
        PlaceSettlement("Sydney", "New South Wales", "au", "Australia", isPopular = true),
        PlaceSettlement("Berlin", "Berlin", "de", "Germany", isPopular = true),
        PlaceSettlement("Dubai", "Dubai", "ae", "United Arab Emirates", isPopular = true),
        PlaceSettlement("Paris", "Île-de-France", "fr", "France", isPopular = true),
        PlaceSettlement("Tokyo", "Kanto", "jp", "Japan", isPopular = true),
        PlaceSettlement("Seoul", "Seoul", "kr", "South Korea", isPopular = true),
        PlaceSettlement("Singapore", "Singapore", "sg", "Singapore", isPopular = true),
        PlaceSettlement("Kuala Lumpur", "Federal Territory", "my", "Malaysia", isPopular = true)
    )

    private fun normalizeCountryNameForApi(countryCode: String?, countryName: String?): String {
        val code = countryCode?.lowercase()?.trim().orEmpty()
        val rawName = countryName?.trim().orEmpty()
        val mappedByCode = when (code) {
            "us" -> "United States"
            "gb", "uk" -> "United Kingdom"
            "ae" -> "United Arab Emirates"
            "kr" -> "South Korea"
            "kp" -> "North Korea"
            "ru" -> "Russia"
            "vn" -> "Vietnam"
            "tw" -> "Taiwan"
            "cz" -> "Czech Republic"
            "tr" -> "Turkey"
            "ir" -> "Iran"
            "sy" -> "Syria"
            "tz" -> "Tanzania"
            "bo" -> "Bolivia"
            "ve" -> "Venezuela"
            "nl" -> "Netherlands"
            "ph" -> "Philippines"
            "do" -> "Dominican Republic"
            else -> ""
        }
        return when {
            mappedByCode.isNotBlank() -> mappedByCode
            rawName.isNotBlank() -> rawName
            code.isNotBlank() -> Locale("", code.uppercase()).getDisplayCountry(Locale.ENGLISH)
            else -> ""
        }
    }

    fun getPopularCities(countryCode: String?): List<PlaceSettlement> {
        val code = countryCode?.lowercase()?.trim().orEmpty()
        if (code.isBlank()) return emptyList()
        val cached = countryCitiesCache[code]
        if (!cached.isNullOrEmpty()) {
            val pop = cached.filter { it.isPopular }
            return if (pop.isNotEmpty()) pop else cached.take(8)
        }
        val countryPopular = offlineSeedSettlements.filter { it.countryCode == code && it.isPopular }
        if (countryPopular.isNotEmpty()) return countryPopular
        return offlineSeedSettlements.filter { it.countryCode == code }.take(6)
    }

    fun getCitiesForCountry(countryCode: String?): List<PlaceSettlement> {
        val code = countryCode?.lowercase()?.trim().orEmpty()
        if (code.isBlank()) return emptyList()
        return countryCitiesCache[code] ?: offlineSeedSettlements.filter { it.countryCode == code }
    }

    fun searchSettlements(query: String, countryCode: String?): List<PlaceSettlement> {
        val q = query.trim().lowercase()
        val code = countryCode?.lowercase()?.trim().orEmpty()
        if (q.isBlank()) return getCitiesForCountry(code)

        val cachedSearch = searchCache["$code|$q"]
        if (!cachedSearch.isNullOrEmpty()) return cachedSearch

        val base = getCitiesForCountry(code).ifEmpty {
            if (code.isBlank()) offlineSeedSettlements else emptyList()
        }
        return base.filter {
            (code.isBlank() || it.countryCode == code) &&
                (it.name.lowercase().contains(q) ||
                    it.adminRegion.lowercase().contains(q) ||
                    it.countryName.lowercase().contains(q))
        }
    }

    /**
     * Fetches all cities for a given country using the free CountriesNow API (zero API key),
     * caching the result per country code for instant subsequent lookups.
     */
    suspend fun fetchCitiesForCountry(
        countryCode: String?,
        countryName: String?
    ): List<PlaceSettlement> = withContext(Dispatchers.IO) {
        val code = countryCode?.lowercase()?.trim().orEmpty()
        val apiCountry = normalizeCountryNameForApi(code, countryName)
        val displayCountry = countryName?.trim().takeUnless { it.isNullOrBlank() } ?: apiCountry
        if (code.isBlank() && apiCountry.isBlank()) return@withContext emptyList()

        countryCitiesCache[code]?.takeIf { it.isNotEmpty() }?.let { return@withContext it }

        val fetchedNames = LinkedHashSet<String>()
        if (apiCountry.isNotBlank()) {
            try {
                val payload = JSONObject().put("country", apiCountry).toString()
                val req = Request.Builder()
                    .url("https://countriesnow.space/api/v0.1/countries/cities")
                    .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .header("Accept", "application/json")
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string().orEmpty()
                        if (body.isNotBlank()) {
                            val json = JSONObject(body)
                            if (!json.optBoolean("error", false)) {
                                val arr = json.optJSONArray("data")
                                if (arr != null) {
                                    for (i in 0 until arr.length()) {
                                        val city = arr.optString(i).trim()
                                        if (city.isNotBlank()) {
                                            fetchedNames.add(city)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Fall back gracefully below
            }
        }

        if (fetchedNames.isNotEmpty()) {
            val offlineKnown = offlineSeedSettlements
                .filter { it.countryCode == code }
                .associateBy { it.name.lowercase() }
            val result = fetchedNames.mapIndexed { idx, cityName ->
                val seed = offlineKnown[cityName.lowercase()]
                PlaceSettlement(
                    name = cityName,
                    adminRegion = seed?.adminRegion ?: displayCountry,
                    countryCode = code,
                    countryName = displayCountry,
                    isPopular = seed?.isPopular == true || idx < 8
                )
            }
            if (code.isNotBlank()) {
                countryCitiesCache[code] = result
            }
            return@withContext result
        }

        offlineSeedSettlements.filter { it.countryCode == code }
    }

    suspend fun fetchPopularCities(
        countryCode: String?,
        countryName: String?
    ): List<PlaceSettlement> {
        val all = fetchCitiesForCountry(countryCode, countryName)
        val popular = all.filter { it.isPopular }
        return if (popular.isNotEmpty()) popular.take(10) else all.take(8)
    }

    /**
     * Global or country-scoped fuzzy city search via Open-Meteo Geocoding API (free, no key required),
     * merged with cached country city lists so partial queries like "lahor" match "Lahore".
     */
    suspend fun searchCitiesOnline(
        query: String,
        countryCode: String? = null,
        countryName: String? = null
    ): List<PlaceSettlement> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        val code = countryCode?.lowercase()?.trim().orEmpty()
        if (trimmed.isBlank()) {
            return@withContext if (code.isNotBlank()) {
                fetchCitiesForCountry(code, countryName)
            } else {
                offlineSeedSettlements
            }
        }

        val cacheKey = "$code|${trimmed.lowercase()}"
        searchCache[cacheKey]?.let { return@withContext it }

        val merged = LinkedHashMap<String, PlaceSettlement>()
        val queryCandidates = buildList {
            add(trimmed)
            // Also try trimming last char / normalized form if Open-Meteo is queried with short stem
            if (trimmed.length >= 4) add(trimmed.dropLast(1))
        }.distinct()

        for (candidate in queryCandidates) {
            if (merged.size >= 15) break
            try {
                val encoded = URLEncoder.encode(candidate, "UTF-8")
                val url = "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=30&language=en&format=json"
                val req = Request.Builder()
                    .url(url)
                    .get()
                    .header("Accept", "application/json")
                    .build()
                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val raw = resp.body?.string().orEmpty()
                        if (raw.isNotBlank()) {
                            val json = JSONObject(raw)
                            val results = json.optJSONArray("results")
                            if (results != null) {
                                val parsed = mutableListOf<Pair<Long, PlaceSettlement>>()
                                for (i in 0 until results.length()) {
                                    val obj = results.optJSONObject(i) ?: continue
                                    val cityName = obj.optString("name").trim()
                                    if (cityName.isBlank()) continue
                                    val resCountryCode = obj.optString("country_code").lowercase().trim()
                                    if (code.isNotBlank() && resCountryCode != code) continue
                                    val resCountryName = obj.optString("country").trim()
                                        .ifBlank { countryName?.takeIf { code.isNotBlank() }.orEmpty() }
                                    val admin1 = obj.optString("admin1").trim()
                                        .ifBlank { resCountryName }
                                    val population = obj.optLong("population", 0L)
                                    val settlement = PlaceSettlement(
                                        name = cityName,
                                        adminRegion = admin1,
                                        countryCode = resCountryCode,
                                        countryName = resCountryName,
                                        isPopular = population >= 500_000L
                                    )
                                    parsed.add(population to settlement)
                                }
                                parsed.sortedByDescending { it.first }.forEach { (_, s) ->
                                    val key = "${s.name.lowercase()}|${s.adminRegion.lowercase()}|${s.countryCode}"
                                    merged.putIfAbsent(key, s)
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Ignore transient error and check local/cached fallback
            }
            if (merged.isNotEmpty()) break
        }

        // Also match from cached country cities list (prefix / substring)
        val cachedOrSeed = if (code.isNotBlank()) {
            fetchCitiesForCountry(code, countryName)
        } else {
            offlineSeedSettlements
        }
        val qLower = trimmed.lowercase()
        cachedOrSeed.filter {
            it.name.lowercase().contains(qLower) || it.adminRegion.lowercase().contains(qLower)
        }.forEach { s ->
            val key = "${s.name.lowercase()}|${s.adminRegion.lowercase()}|${s.countryCode}"
            merged.putIfAbsent(key, s)
        }

        val finalResults = merged.values.toList().take(40)
        if (finalResults.isNotEmpty()) {
            searchCache[cacheKey] = finalResults
        }
        finalResults
    }

    @SuppressLint("MissingPermission")
    suspend fun detectCurrentLocation(context: Context): PlaceSettlement? = withContext(Dispatchers.IO) {
        try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@withContext null
            val providers = lm.getProviders(true)
            var bestLocation: Location? = null

            for (provider in providers) {
                val l = try { lm.getLastKnownLocation(provider) } catch (_: SecurityException) { null } ?: continue
                if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                    bestLocation = l
                }
            }

            if (bestLocation != null && Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(bestLocation.latitude, bestLocation.longitude, 1)
                val addr = addresses?.firstOrNull()
                if (addr != null) {
                    val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea
                    if (city != null) {
                        val state = addr.adminArea ?: ""
                        val countryCode = addr.countryCode?.lowercase() ?: ""
                        val countryName = addr.countryName ?: ""
                        return@withContext PlaceSettlement(
                            name = city,
                            adminRegion = state,
                            countryCode = countryCode,
                            countryName = countryName,
                            isPopular = false
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        return@withContext null
    }
}
