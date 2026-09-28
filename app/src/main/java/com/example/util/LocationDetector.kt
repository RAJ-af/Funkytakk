package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

object LocationDetector {
    private const val TAG = "LocationDetector"

    data class DetectedLocation(
        val city: String,
        val country: String,
        val countryCode: String,
        val fullString: String
    )

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    suspend fun detectCurrentLocation(context: Context): DetectedLocation = withContext(Dispatchers.IO) {
        try {
            if (hasLocationPermission(context)) {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                if (locationManager != null) {
                    val providers = listOf(
                        LocationManager.GPS_PROVIDER,
                        LocationManager.NETWORK_PROVIDER,
                        LocationManager.PASSIVE_PROVIDER
                    )
                    var bestLocation: Location? = null
                    for (provider in providers) {
                        try {
                            if (locationManager.isProviderEnabled(provider)) {
                                val loc = locationManager.getLastKnownLocation(provider)
                                if (loc != null) {
                                    if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                                        bestLocation = loc
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Provider $provider failed: ${e.message}")
                        }
                    }

                    if (bestLocation != null) {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val resolved = kotlinx.coroutines.withTimeoutOrNull(2500L) {
                            resolveGeocoder(geocoder, bestLocation)
                        }
                        if (resolved != null) {
                            return@withContext resolved
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in detectCurrentLocation", e)
        }

        // Clean locale and timezone-aware default if GPS hardware coordinates are unavailable
        val defaultLocale = Locale.getDefault()
        val tzId = java.util.TimeZone.getDefault().id.lowercase()
        val detectedCountryCode = when {
            defaultLocale.country.isNotBlank() -> defaultLocale.country.lowercase()
            tzId.contains("calcutta") || tzId.contains("kolkata") || tzId.contains("india") -> "in"
            tzId.contains("new_york") || tzId.contains("los_angeles") || tzId.contains("chicago") -> "us"
            tzId.contains("london") -> "gb"
            tzId.contains("dubai") -> "ae"
            tzId.contains("tokyo") -> "jp"
            tzId.contains("seoul") -> "kr"
            tzId.contains("dhaka") -> "bd"
            tzId.contains("karachi") -> "pk"
            tzId.contains("madrid") -> "es"
            tzId.contains("berlin") -> "de"
            tzId.contains("paris") -> "fr"
            tzId.contains("kuala_lumpur") -> "my"
            else -> "in"
        }

        val (city, country) = when (detectedCountryCode) {
            "in" -> "New Delhi" to "India"
            "us" -> "New York" to "United States"
            "gb" -> "London" to "United Kingdom"
            "ae" -> "Dubai" to "United Arab Emirates"
            "jp" -> "Tokyo" to "Japan"
            "kr" -> "Seoul" to "South Korea"
            "es" -> "Madrid" to "Spain"
            "de" -> "Berlin" to "Germany"
            "fr" -> "Paris" to "France"
            "pk" -> "Islamabad" to "Pakistan"
            "bd" -> "Dhaka" to "Bangladesh"
            "my" -> "Kuala Lumpur" to "Malaysia"
            "sg" -> "Singapore" to "Singapore"
            "ca" -> "Toronto" to "Canada"
            "au" -> "Sydney" to "Australia"
            else -> "New Delhi" to "India"
        }

        return@withContext DetectedLocation(
            city = city,
            country = country,
            countryCode = detectedCountryCode,
            fullString = "$city, $country"
        )
    }

    private suspend fun resolveGeocoder(geocoder: Geocoder, location: Location): DetectedLocation? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                try {
                    geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                        val addr = addresses.firstOrNull()
                        if (addr != null) {
                            val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "Kuala Lumpur"
                            val country = addr.countryName ?: "Malaysia"
                            val code = addr.countryCode?.lowercase() ?: "my"
                            if (continuation.isActive) {
                                continuation.resume(DetectedLocation(city, country, code, "$city, $country"))
                            }
                        } else {
                            if (continuation.isActive) continuation.resume(null)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Tiramisu Geocoder error", e)
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        } else {
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                val addr = addresses?.firstOrNull()
                if (addr != null) {
                    val city = addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: "Kuala Lumpur"
                    val country = addr.countryName ?: "Malaysia"
                    val code = addr.countryCode?.lowercase() ?: "my"
                    DetectedLocation(city, country, code, "$city, $country")
                } else null
            } catch (e: Exception) {
                Log.e(TAG, "Legacy Geocoder error", e)
                null
            }
        }
    }
}
