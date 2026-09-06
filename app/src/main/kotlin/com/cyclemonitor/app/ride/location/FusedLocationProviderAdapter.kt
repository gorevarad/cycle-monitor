package com.cyclemonitor.app.ride.location

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.cyclemonitor.core.location.LocationAccuracyMode
import com.cyclemonitor.core.location.LocationAvailability
import com.cyclemonitor.core.location.LocationProvider
import com.cyclemonitor.core.model.LocationSample
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Production [LocationProvider] wrapping Android's FusedLocationProviderClient. This is the only
 * place in the app that talks to real location hardware -- everything above it (RideEngine,
 * ViewModels, UI) only ever sees the platform-agnostic [LocationSample]/[LocationProvider]
 * contract from `:core`, so this class can be swapped for
 * [com.cyclemonitor.core.location.MockLocationProvider] in debug builds without touching anything
 * else.
 */
class FusedLocationProviderAdapter(
    context: Context,
    private val accuracyMode: LocationAccuracyMode = LocationAccuracyMode.NORMAL,
) : LocationProvider {

    private val appContext = context.applicationContext
    private val client = LocationServices.getFusedLocationProviderClient(appContext)

    private fun hasFineLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    override fun locationUpdates(): Flow<LocationSample> = callbackFlow {
        if (!hasFineLocationPermission()) {
            close(SecurityException("ACCESS_FINE_LOCATION not granted"))
            return@callbackFlow
        }

        val (priority, intervalMillis) = when (accuracyMode) {
            LocationAccuracyMode.BATTERY_SAVER -> Priority.PRIORITY_BALANCED_POWER_ACCURACY to 5000L
            LocationAccuracyMode.NORMAL -> Priority.PRIORITY_HIGH_ACCURACY to 1500L
            LocationAccuracyMode.HIGH_ACCURACY -> Priority.PRIORITY_HIGH_ACCURACY to 1000L
        }
        val request = LocationRequest.Builder(priority, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                trySend(
                    LocationSample(
                        timestampMillis = location.time,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        horizontalAccuracyMeters = if (location.hasAccuracy()) location.accuracy else null,
                        reportedSpeedMps = if (location.hasSpeed()) location.speed else null,
                        speedAccuracyMps = if (
                            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                            location.hasSpeedAccuracy()
                        ) {
                            location.speedAccuracyMetersPerSecond
                        } else {
                            null
                        },
                        bearingDegrees = if (location.hasBearing()) location.bearing else null,
                        altitudeMeters = if (location.hasAltitude()) location.altitude else null,
                        altitudeAccuracyMeters = if (
                            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
                            location.hasVerticalAccuracy()
                        ) {
                            location.verticalAccuracyMeters
                        } else {
                            null
                        },
                        isMocked = false,
                    ),
                )
            }
        }

        client.requestLocationUpdates(request, callback, appContext.mainLooper)
        awaitClose { client.removeLocationUpdates(callback) }
    }

    override fun availability(): Flow<LocationAvailability> = callbackFlow {
        fun currentAvailability(): LocationAvailability {
            if (!hasFineLocationPermission()) return LocationAvailability.PERMISSION_DENIED
            val locationManager = appContext.getSystemService<LocationManager>()
            val enabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
            return if (enabled) LocationAvailability.AVAILABLE else LocationAvailability.PROVIDER_DISABLED
        }
        trySend(currentAvailability())

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(currentAvailability())
            }
        }
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        awaitClose { appContext.unregisterReceiver(receiver) }
    }
}
