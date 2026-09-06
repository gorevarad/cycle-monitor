package com.cyclemonitor.app.routing

import com.cyclemonitor.core.navigation.NavRoute
import com.cyclemonitor.core.navigation.NavStep
import com.cyclemonitor.core.navigation.NavigationProvider
import com.cyclemonitor.core.navigation.RoutePoint
import com.cyclemonitor.core.navigation.RouteRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Real cycling-aware routing via the Google Maps Platform Directions API (bicycling travel
 * mode). This is a separate REST product from the Maps SDK used for map display -- it needs its
 * own key (the same Maps API key works if the Directions API is also enabled for it in Google
 * Cloud Console) and, unlike the Maps SDK, has no official Kotlin/Compose client, so this talks
 * to the plain JSON REST endpoint directly with HttpURLConnection + org.json (both built into the
 * Android platform, no extra HTTP/JSON library dependency).
 *
 * Falls back to failure (never a fabricated route) if the network call fails, the API returns a
 * non-OK status, or no key is configured -- callers should have
 * [com.cyclemonitor.app.map.MapAvailability.isConfigured] gate whether this provider or
 * [com.cyclemonitor.core.navigation.StraightLineNavigationProvider] is used.
 */
class GoogleDirectionsNavigationProvider(private val apiKey: String) : NavigationProvider {

    override suspend fun calculateRoute(request: RouteRequest): Result<NavRoute> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(IllegalStateException("No Maps API key configured"))
        try {
            val url = buildUrl(request)
            val response = fetch(url)
            Result.success(parseResponse(response))
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: DirectionsApiException) {
            Result.failure(e)
        }
    }

    private fun buildUrl(request: RouteRequest): URL {
        fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
        val origin = "${request.origin.latitude},${request.origin.longitude}"
        val destination = "${request.destination.latitude},${request.destination.longitude}"
        val urlString = "https://maps.googleapis.com/maps/api/directions/json" +
            "?origin=${encode(origin)}&destination=${encode(destination)}&mode=bicycling&key=${encode(apiKey)}"
        return URL(urlString)
    }

    private fun fetch(url: URL): String {
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.requestMethod = "GET"
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Directions API HTTP ${connection.responseCode}")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private class DirectionsApiException(message: String) : Exception(message)

    private fun parseResponse(json: String): NavRoute {
        val root = JSONObject(json)
        val status = root.optString("status", "UNKNOWN")
        if (status != "OK") {
            throw DirectionsApiException("Directions API status: $status")
        }
        val route = root.getJSONArray("routes").getJSONObject(0)
        val overviewPolyline = route.getJSONObject("overview_polyline").getString("points")
        val points = PolylineDecoder.decode(overviewPolyline).map { (lat, lng) -> RoutePoint(lat, lng) }

        val legs = route.getJSONArray("legs")
        var totalDistanceMeters = 0.0
        var totalDurationSeconds = 0L
        val steps = mutableListOf<NavStep>()
        for (legIndex in 0 until legs.length()) {
            val leg = legs.getJSONObject(legIndex)
            totalDistanceMeters += leg.getJSONObject("distance").getDouble("value")
            totalDurationSeconds += leg.getJSONObject("duration").getLong("value")
            val legSteps = leg.getJSONArray("steps")
            for (stepIndex in 0 until legSteps.length()) {
                val step = legSteps.getJSONObject(stepIndex)
                val instruction = stripHtml(step.optString("html_instructions", ""))
                val distance = step.getJSONObject("distance").getDouble("value")
                steps += NavStep(instruction = instruction, distanceMeters = distance)
            }
        }

        return NavRoute(
            points = points,
            distanceMeters = totalDistanceMeters,
            estimatedDurationSeconds = totalDurationSeconds,
            steps = steps,
            isApproximate = false,
        )
    }

    private fun stripHtml(value: String): String = value.replace(Regex("<[^>]*>"), "").trim()
}
