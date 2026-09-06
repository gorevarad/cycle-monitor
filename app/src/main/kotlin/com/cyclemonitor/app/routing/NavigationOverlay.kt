package com.cyclemonitor.app.routing

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.cyclemonitor.app.ride.ui.Formatters
import com.cyclemonitor.app.theme.CycleColors
import com.cyclemonitor.core.units.DistanceUnit

/**
 * Overlays the ride map with ROUTE PLANNING affordances: a search entry point when idle, and a
 * glanceable turn-by-turn banner (upcoming instruction, distance remaining, ETA, re-route/cancel)
 * once a route is active. See RideNavigationViewModel for the state machine.
 */
@Composable
fun NavigationOverlay(
    state: RideNavigationUiState,
    distanceUnit: DistanceUnit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSelectResult: (SearchResult) -> Unit,
    onCancelNavigation: () -> Unit,
    onReroute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (state.isNavigating) {
            NavigationBanner(
                state = state,
                distanceUnit = distanceUnit,
                onCancel = onCancelNavigation,
                onReroute = onReroute,
                modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
            )
        } else {
            FilledIconButton(onClick = onOpenSearch, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                Icon(Icons.Filled.Navigation, contentDescription = "Navigate to destination")
            }
        }
    }

    if (state.isSearchOpen) {
        DestinationSearchDialog(
            query = state.query,
            results = state.results,
            isSearching = state.isSearching,
            onQueryChange = onQueryChange,
            onSelectResult = onSelectResult,
            onDismiss = onCloseSearch,
        )
    }
}

@Composable
private fun NavigationBanner(
    state: RideNavigationUiState,
    distanceUnit: DistanceUnit,
    onCancel: () -> Unit,
    onReroute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val route = state.activeRoute
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CycleColors.SurfaceRaised, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                state.destinationName ?: "Navigating",
                style = MaterialTheme.typography.labelLarge,
                color = CycleColors.TextPrimary,
            )
            Row {
                IconButton(onClick = onReroute) { Icon(Icons.Filled.Refresh, contentDescription = "Re-route") }
                IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, contentDescription = "Cancel navigation") }
            }
        }
        if (route != null) {
            Text(
                route.steps.firstOrNull()?.instruction ?: "Continue toward destination",
                style = MaterialTheme.typography.bodyMedium,
                color = CycleColors.TextSecondary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    "${Formatters.distance(distanceUnit.fromMeters(route.distanceMeters), distanceUnit.symbol())} remaining",
                    style = MaterialTheme.typography.bodySmall,
                    color = CycleColors.TextPrimary,
                )
                route.estimatedDurationSeconds?.let {
                    Text("ETA ${Formatters.duration(it)}", style = MaterialTheme.typography.bodySmall, color = CycleColors.TextPrimary)
                }
            }
            if (route.isApproximate) {
                Text(
                    "Approximate direct line -- no routing backend configured",
                    style = MaterialTheme.typography.labelSmall,
                    color = CycleColors.StatusAmber,
                )
            }
        }
        state.error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = CycleColors.StatusRed)
        }
    }
}

@Composable
private fun DestinationSearchDialog(
    query: String,
    results: List<SearchResult>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSelectResult: (SearchResult) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CycleColors.SurfaceCharcoal, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("SEARCH DESTINATION", style = MaterialTheme.typography.titleMedium, color = CycleColors.TextPrimary)
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close") }
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Address, city, landmark...") },
            )
            if (isSearching) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
            LazyColumn {
                items(results) { result ->
                    Text(
                        result.displayName,
                        color = CycleColors.TextPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectResult(result) }
                            .padding(vertical = 12.dp),
                    )
                }
            }
        }
    }
}
