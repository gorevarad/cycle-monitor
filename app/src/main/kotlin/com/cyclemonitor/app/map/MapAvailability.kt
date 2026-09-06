package com.cyclemonitor.app.map

import com.cyclemonitor.app.BuildConfig

/**
 * Whether a Google Maps API key was supplied at build time (see app/build.gradle.kts, which
 * reads MAPS_API_KEY from local.properties). The map panel must never crash or silently show a
 * blank gray box when this is false -- it shows an explicit "map unavailable" state instead, and
 * ride recording must keep working regardless either way (RideRecordingService has no dependency
 * on the map at all).
 */
object MapAvailability {
    val isConfigured: Boolean get() = BuildConfig.MAPS_CONFIGURED
}
