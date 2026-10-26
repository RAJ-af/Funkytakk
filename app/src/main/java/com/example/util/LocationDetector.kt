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

    fun isGpsEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        val gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val net = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return gps || net
    }

    suspend fun detectCurrentLocation(context: Context): DetectedLocation? = withContext(Dispatchers.IO) {
        try {
            if (hasLocationPermission(context)) {
                val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                if (locationManager != null && isGpsEnabled(context)) {
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
                        val resolved = kotlinx.coroutines.withTimeoutOrNull(4000L) {
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

        return@withContext null
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
