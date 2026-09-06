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
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.cyclemonitor.app.theme.CycleColors
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

data class RiderPosition(val latitude: Double, val longitude: Double, val bearingDegrees: Float?)

@Composable
fun RideMapPanel(
    position: RiderPosition?,
    route: List<RiderPosition> = emptyList(),
    modifier: Modifier = Modifier,
) {
    if (!MapAvailability.isConfigured) {
        MapUnavailablePlaceholder(modifier)
        return
    }
    GoogleMapContent(position, route, modifier)
}

@Composable
private fun GoogleMapContent(
    position: RiderPosition?,
    route: List<RiderPosition>,
    modifier: Modifier,
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
            properties = MapProperties(isMyLocationEnabled = false),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = false,
                mapToolbarEnabled = false,
            ),
        ) {
            if (route.size >= 2) {
                Polyline(
                    points = route.map { LatLng(it.latitude, it.longitude) },
                    width = 8f,
                )
            }
            position?.let { p ->
                val markerState = remember(p.latitude, p.longitude) {
                    MarkerState(position = LatLng(p.latitude, p.longitude))
                }
                Marker(
                    state = markerState,
                    rotation = p.bearingDegrees ?: 0f,
                    flat = true,
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
            Text("No Maps API key configured", color = CycleColors.TextDisabled, style = MaterialTheme.typography.bodySmall)
        }
    }
}
