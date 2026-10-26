package com.example.ui.screens.profile.components

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class PlaceSettlement(
    val name: String,
    val adminRegion: String,
    val countryCode: String,
    val countryName: String,
    val isPopular: Boolean = false
)

object PlaceDbService {

    private val settlements: List<PlaceSettlement> = listOf(
        // INDIA
        PlaceSettlement("New Delhi", "National Capital Territory", "in", "India", isPopular = true),
        PlaceSettlement("Mumbai", "Maharashtra", "in", "India", isPopular = true),
        PlaceSettlement("Bengaluru", "Karnataka", "in", "India", isPopular = true),
        PlaceSettlement("Hyderabad", "Telangana", "in", "India", isPopular = true),
        PlaceSettlement("Chennai", "Tamil Nadu", "in", "India", isPopular = true),
        PlaceSettlement("Kolkata", "West Bengal", "in", "India", isPopular = true),
        PlaceSettlement("Pune", "Maharashtra", "in", "India", isPopular = true),
        PlaceSettlement("Ghaziabad", "Uttar Pradesh", "in", "India", isPopular = false),
        PlaceSettlement("Agra", "Uttar Pradesh", "in", "India", isPopular = false),
        PlaceSettlement("Ahmedabad", "Gujarat", "in", "India", isPopular = false),
        PlaceSettlement("Ajmer", "Rajasthan", "in", "India", isPopular = false),
        PlaceSettlement("Aligarh", "Uttar Pradesh", "in", "India", isPopular = false),
        PlaceSettlement("Amritsar", "Punjab", "in", "India", isPopular = false),
        PlaceSettlement("Bhopal", "Madhya Pradesh", "in", "India", isPopular = false),
        PlaceSettlement("Chandigarh", "Chandigarh", "in", "India", isPopular = false),
        PlaceSettlement("Coimbatore", "Tamil Nadu", "in", "India", isPopular = false),
        PlaceSettlement("Dehradun", "Uttarakhand", "in", "India", isPopular = false),
        PlaceSettlement("Faridabad", "Haryana", "in", "India", isPopular = false),
        PlaceSettlement("Gurugram", "Haryana", "in", "India", isPopular = false),
        PlaceSettlement("Guwahati", "Assam", "in", "India", isPopular = false),
        PlaceSettlement("Gwalior", "Madhya Pradesh", "in", "India", isPopular = false),
        PlaceSettlement("Indore", "Madhya Pradesh", "in", "India", isPopular = false),
        PlaceSettlement("Jabalpur", "Madhya Pradesh", "in", "India", isPopular = false),
        PlaceSettlement("Jaipur", "Rajasthan", "in", "India", isPopular = false),
        PlaceSettlement("Jodhpur", "Rajasthan", "in", "India", isPopular = false),
        PlaceSettlement("Kanpur", "Uttar Pradesh", "in", "India", isPopular = false),
        PlaceSettlement("Kochi", "Kerala", "in", "India", isPopular = false),
        PlaceSettlement("Kota", "Rajasthan", "in", "India", isPopular = false),
        PlaceSettlement("Lucknow", "Uttar Pradesh", "in", "India", isPopular = false),
        PlaceSettlement("Ludhiana", "Punjab", "in", "India", isPopular = false),
        PlaceSettlement("Madurai", "Tamil Nadu", "in", "India", isPopular = false),
        PlaceSettlement("Meerut", "Uttar Pradesh", "in", "India", isPopular = false),
        PlaceSettlement("Nagpur", "Maharashtra", "in", "India", isPopular = false),
        PlaceSettlement("Nashik", "Maharashtra", "in", "India", isPopular = false),
        PlaceSettlement("Noida", "Uttar Pradesh", "in", "India", isPopular = false),
        PlaceSettlement("Patna", "Bihar", "in", "India", isPopular = false),
        PlaceSettlement("Raipur", "Chhattisgarh", "in", "India", isPopular = false),
        PlaceSettlement("Rajkot", "Gujarat", "in", "India", isPopular = false),
        PlaceSettlement("Ranchi", "Jharkhand", "in", "India", isPopular = false),
        PlaceSettlement("Surat", "Gujarat", "in", "India", isPopular = false),
        PlaceSettlement("Thane", "Maharashtra", "in", "India", isPopular = false),
        PlaceSettlement("Thiruvananthapuram", "Kerala", "in", "India", isPopular = false),
        PlaceSettlement("Vadodara", "Gujarat", "in", "India", isPopular = false),
        PlaceSettlement("Varanasi", "Uttar Pradesh", "in", "India", isPopular = false),
        PlaceSettlement("Vijayawada", "Andhra Pradesh", "in", "India", isPopular = false),
        PlaceSettlement("Visakhapatnam", "Andhra Pradesh", "in", "India", isPopular = false),

        // UNITED STATES
        PlaceSettlement("New York", "New York", "us", "United States", isPopular = true),
        PlaceSettlement("Los Angeles", "California", "us", "United States", isPopular = true),
        PlaceSettlement("Chicago", "Illinois", "us", "United States", isPopular = true),
        PlaceSettlement("Houston", "Texas", "us", "United States", isPopular = true),
        PlaceSettlement("San Francisco", "California", "us", "United States", isPopular = true),
        PlaceSettlement("Seattle", "Washington", "us", "United States", isPopular = true),
        PlaceSettlement("Miami", "Florida", "us", "United States", isPopular = true),
        PlaceSettlement("Austin", "Texas", "us", "United States", isPopular = false),
        PlaceSettlement("Boston", "Massachusetts", "us", "United States", isPopular = false),
        PlaceSettlement("Dallas", "Texas", "us", "United States", isPopular = false),
        PlaceSettlement("Denver", "Colorado", "us", "United States", isPopular = false),
        PlaceSettlement("Las Vegas", "Nevada", "us", "United States", isPopular = false),
        PlaceSettlement("Philadelphia", "Pennsylvania", "us", "United States", isPopular = false),
        PlaceSettlement("Phoenix", "Arizona", "us", "United States", isPopular = false),
        PlaceSettlement("San Diego", "California", "us", "United States", isPopular = false),
        PlaceSettlement("Washington D.C.", "District of Columbia", "us", "United States", isPopular = false),

        // UNITED KINGDOM
        PlaceSettlement("London", "Greater London", "gb", "United Kingdom", isPopular = true),
        PlaceSettlement("Manchester", "Greater Manchester", "gb", "United Kingdom", isPopular = true),
        PlaceSettlement("Birmingham", "West Midlands", "gb", "United Kingdom", isPopular = true),
        PlaceSettlement("Edinburgh", "Scotland", "gb", "United Kingdom", isPopular = true),
        PlaceSettlement("Glasgow", "Scotland", "gb", "United Kingdom", isPopular = true),
        PlaceSettlement("Liverpool", "Merseyside", "gb", "United Kingdom", isPopular = false),
        PlaceSettlement("Bristol", "South West England", "gb", "United Kingdom", isPopular = false),
        PlaceSettlement("Leeds", "West Yorkshire", "gb", "United Kingdom", isPopular = false),
        PlaceSettlement("Cambridge", "Cambridgeshire", "gb", "United Kingdom", isPopular = false),
        PlaceSettlement("Oxford", "Oxfordshire", "gb", "United Kingdom", isPopular = false),

        // CANADA
        PlaceSettlement("Toronto", "Ontario", "ca", "Canada", isPopular = true),
        PlaceSettlement("Vancouver", "British Columbia", "ca", "Canada", isPopular = true),
        PlaceSettlement("Montreal", "Quebec", "ca", "Canada", isPopular = true),
        PlaceSettlement("Calgary", "Alberta", "ca", "Canada", isPopular = true),
        PlaceSettlement("Ottawa", "Ontario", "ca", "Canada", isPopular = true),

        // AUSTRALIA
        PlaceSettlement("Sydney", "New South Wales", "au", "Australia", isPopular = true),
        PlaceSettlement("Melbourne", "Victoria", "au", "Australia", isPopular = true),
        PlaceSettlement("Brisbane", "Queensland", "au", "Australia", isPopular = true),
        PlaceSettlement("Perth", "Western Australia", "au", "Australia", isPopular = true),
        PlaceSettlement("Adelaide", "South Australia", "au", "Australia", isPopular = false),

        // GERMANY
        PlaceSettlement("Berlin", "Berlin", "de", "Germany", isPopular = true),
        PlaceSettlement("Munich", "Bavaria", "de", "Germany", isPopular = true),
        PlaceSettlement("Hamburg", "Hamburg", "de", "Germany", isPopular = true),
        PlaceSettlement("Frankfurt", "Hesse", "de", "Germany", isPopular = true),

        // UNITED ARAB EMIRATES
        PlaceSettlement("Dubai", "Dubai", "ae", "United Arab Emirates", isPopular = true),
        PlaceSettlement("Abu Dhabi", "Abu Dhabi", "ae", "United Arab Emirates", isPopular = true),
        PlaceSettlement("Sharjah", "Sharjah", "ae", "United Arab Emirates", isPopular = true),
        PlaceSettlement("Ajman", "Ajman", "ae", "United Arab Emirates", isPopular = false),

        // FRANCE
        PlaceSettlement("Paris", "Île-de-France", "fr", "France", isPopular = true),
        PlaceSettlement("Marseille", "Provence-Alpes-Côte d'Azur", "fr", "France", isPopular = true),
        PlaceSettlement("Lyon", "Auvergne-Rhône-Alpes", "fr", "France", isPopular = true),

        // JAPAN
        PlaceSettlement("Tokyo", "Kanto", "jp", "Japan", isPopular = true),
        PlaceSettlement("Osaka", "Kansai", "jp", "Japan", isPopular = true),
        PlaceSettlement("Kyoto", "Kansai", "jp", "Japan", isPopular = true)
    )

    fun getPopularCities(countryCode: String?): List<PlaceSettlement> {
        val code = countryCode?.lowercase()?.trim().orEmpty()
        if (code.isBlank()) return emptyList()
        val countryPopular = settlements.filter { it.countryCode == code && it.isPopular }
        if (countryPopular.isNotEmpty()) return countryPopular

        val allOfCountry = settlements.filter { it.countryCode == code }
        return allOfCountry.take(6)
    }

    fun getCitiesForCountry(countryCode: String?): List<PlaceSettlement> {
        val code = countryCode?.lowercase()?.trim().orEmpty()
        if (code.isBlank()) return emptyList()
        return settlements.filter { it.countryCode == code }
    }

    fun searchSettlements(query: String, countryCode: String?): List<PlaceSettlement> {
        val q = query.trim().lowercase()
        val code = countryCode?.lowercase()?.trim().orEmpty()

        if (q.isBlank()) {
            return getCitiesForCountry(code)
        }

        return settlements.filter {
            (code.isBlank() || it.countryCode == code) &&
                (it.name.lowercase().contains(q) || it.adminRegion.lowercase().contains(q))
        }
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
