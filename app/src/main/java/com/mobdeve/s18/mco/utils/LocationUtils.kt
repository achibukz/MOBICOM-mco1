package com.mobdeve.s18.mco.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat

class LocationUtils {

    companion object {
        fun hasLocationPermission(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

        fun getCurrentLocation(context: Context): Location? {
            if (!hasLocationPermission(context)) return null

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            return try {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } catch (e: SecurityException) {
                null
            }
        }

        fun formatCoordinates(latitude: Double, longitude: Double): String {
            return String.format("%.6f, %.6f", latitude, longitude)
        }

        // Default location (Manila, Philippines) for demo purposes
        fun getDefaultLocation(): Pair<Double, Double> {
            return Pair(14.5995, 120.9842)
        }
    }
}
