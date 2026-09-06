package com.cyclemonitor.app.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.cyclemonitor.app.data.settings.MapStyle
import com.cyclemonitor.app.theme.CycleColors
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

data class RiderPosition(val latitude: Double, val longitude: Double, val bearingDegrees: Float?)

/**
 * The dashboard's center map panel. Shows a real Google Map with the rider's current position
 * when a Maps API key is configured ([MapAvailability.isConfigured]); otherwise shows an honest
 * "map unavailable" placeholder rather than a blank/broken view. Ride recording never depends on
 * this composable at all -- it can fail or never render without affecting the ride.
 *
 * [routePoints]/[destination] optionally draw an active navigation route (see
 * com.cyclemonitor.app.routing.RideNavigationViewModel) -- both default to "no route" so this
 * composable works identically for the plain ride dashboard and for turn-by-turn navigation.
 */
@Composable
fun RideMapPanel(
    position: RiderPosition?,
    modifier: Modifier = Modifier,
    routePoints: List<Pair<Double, Double>> = emptyList(),
    destination: Pair<Double, Double>? = null,
    mapStyle: MapStyle = MapStyle.STANDARD,
) {
    if (!MapAvailability.isConfigured) {
        MapUnavailablePlaceholder(modifier)
        return
    }
    GoogleMapContent(position, modifier, routePoints, destination, mapStyle)
}

private fun MapStyle.toMapType(): MapType = when (this) {
    MapStyle.STANDARD -> MapType.NORMAL
    MapStyle.SATELLITE -> MapType.SATELLITE
    MapStyle.TERRAIN -> MapType.TERRAIN
    // A true night-mode JSON style wasn't included (see README) -- falls back to standard rather
    // than fabricating an unverified style definition.
    MapStyle.NIGHT -> MapType.NORMAL
}

@Composable
private fun GoogleMapContent(
    position: RiderPosition?,
    modifier: Modifier,
    routePoints: List<Pair<Double, Double>>,
    destination: Pair<Double, Double>?,
    mapStyle: MapStyle,
) {
    val defaultLatLng = remember { LatLng(0.0, 0.0) }
    val cameraPositionState = rememberCameraPositionState {
        this.position = CameraPosition.fromLatLngZoom(defaultLatLng, 16f)
    }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(position?.latitude, position?.longitude) {
        val p = position ?: return@LaunchedEffect
        cameraPositionState.animate(CameraUpdateFactory.newLatLng(LatLng(p.latitude, p.longitude)))
    }

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = false, mapType = mapStyle.toMapType()),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = false,
                mapToolbarEnabled = false,
            ),
        ) {
            if (routePoints.size >= 2) {
                Polyline(
                    points = routePoints.map { (lat, lng) -> LatLng(lat, lng) },
                    color = CycleColors.NavigationCyan,
                    width = 10f,
                )
            }
            destination?.let { (lat, lng) ->
                val destinationMarkerState = remember(lat, lng) { MarkerState(position = LatLng(lat, lng)) }
                Marker(
                    state = destinationMarkerState,
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                )
            }
            position?.let { p ->
                // A fresh MarkerState keyed on the coordinates (rather than rememberMarkerState,
                // which only honors its initial position) so the marker actually moves as the
                // rider's position updates.
                val markerState = remember(p.latitude, p.longitude) { MarkerState(position = LatLng(p.latitude, p.longitude)) }
                Marker(
                    state = markerState,
                    rotation = p.bearingDegrees ?: 0f,
                    flat = true,
                    // Center-anchored so rotation pivots on the rider's position rather than a pin tip.
                    anchor = Offset(0.5f, 0.5f),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN),
                )
            }
        }

        FilledIconButton(
            onClick = {
                val p = position ?: return@FilledIconButton
                coroutineScope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.fromLatLngZoom(LatLng(p.latitude, p.longitude), 16f),
                        ),
                    )
                }
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
        ) {
            Icon(Icons.Filled.MyLocation, contentDescription = "Recenter map")
        }
    }
}

@Composable
private fun MapUnavailablePlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(CycleColors.SurfaceRaised),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Map, contentDescription = null, tint = CycleColors.TextSecondary)
            Text("MAP UNAVAILABLE", color = CycleColors.TextSecondary, style = MaterialTheme.typography.labelLarge)
            Text(
                "No Maps API key configured",
                color = CycleColors.TextDisabled,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
