package com.cyclemonitor.app.routing

import android.content.Context
import android.location.Geocoder
import com.cyclemonitor.core.navigation.RoutePoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

data class SearchResult(val displayName: String, val point: RoutePoint)

/**
 * "Search for destination" (spec: ROUTE PLANNING) via Android's built-in [Geocoder], which uses
 * the device's system geocoding backend (Google Play services on virtually all real devices) --
 * no separate Places API key needed. Uses the synchronous lookup (still supported on every API
 * level this app targets); the async Geocoder callback added in API 33 would avoid a deprecation
 * warning there but isn't required for this to work correctly.
 */
class DestinationSearchService(context: Context) {
    private val geocoder = Geocoder(context.applicationContext, Locale.getDefault())

    suspend fun search(query: String, maxResults: Int = 5): List<SearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank() || !Geocoder.isPresent()) return@withContext emptyList()
        try {
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocationName(query, maxResults) ?: return@withContext emptyList()
            addresses.mapNotNull { address ->
                if (!address.hasLatitude() || !address.hasLongitude()) return@mapNotNull null
                val name = address.getAddressLine(0) ?: "${address.latitude}, ${address.longitude}"
                SearchResult(name, RoutePoint(address.latitude, address.longitude))
            }
        } catch (e: IOException) {
            emptyList()
        }
    }
}
