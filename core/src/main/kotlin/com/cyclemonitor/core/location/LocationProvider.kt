package com.cyclemonitor.core.location

import com.cyclemonitor.core.model.LocationSample
import kotlinx.coroutines.flow.Flow

enum class LocationAvailability {
    AVAILABLE,
    PERMISSION_DENIED,
    PROVIDER_DISABLED,
    UNKNOWN,
}

/**
 * Abstraction over "where is the phone right now". The production implementation wraps
 * Android's FusedLocationProviderClient; [com.cyclemonitor.core.location.MockLocationProvider]
 * implements the same contract for development/testing so the UI and ride-recording logic never
 * need to know which one they're talking to.
 */
interface LocationProvider {
    fun locationUpdates(): Flow<LocationSample>

    fun availability(): Flow<LocationAvailability>
}

/** Tuning knobs for how aggressively location updates are requested; trades battery for accuracy. */
enum class LocationAccuracyMode {
    BATTERY_SAVER,
    NORMAL,
    HIGH_ACCURACY,
}
