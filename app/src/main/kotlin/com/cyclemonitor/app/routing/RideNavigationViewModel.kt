package com.cyclemonitor.app.routing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyclemonitor.core.navigation.NavRoute
import com.cyclemonitor.core.navigation.NavigationProvider
import com.cyclemonitor.core.navigation.RoutePoint
import com.cyclemonitor.core.navigation.RouteRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RideNavigationUiState(
    val isSearchOpen: Boolean = false,
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val isRouting: Boolean = false,
    val activeRoute: NavRoute? = null,
    val destinationName: String? = null,
    val error: String? = null,
) {
    /** Straight-line distance remaining is not accurate turn-by-turn progress -- this is the
     * route's total distance, refreshed on each re-route, not a live "distance remaining" that
     * decrements as the rider moves (that needs continuous off-route detection, out of scope). */
    val isNavigating: Boolean get() = activeRoute != null
}

/**
 * Drives ROUTE PLANNING for the ride screen: destination search, route calculation, and the
 * turn-by-turn info surfaced to [com.cyclemonitor.app.map.RideMapPanel] and its instruction
 * banner. Deliberately separate from map display (see [NavigationProvider] doc) -- this class
 * never touches Compose/Maps types directly.
 */
class RideNavigationViewModel(
    private val navigationProvider: NavigationProvider,
    private val searchService: DestinationSearchService,
) : ViewModel() {

    private val _state = MutableStateFlow(RideNavigationUiState())
    val state: StateFlow<RideNavigationUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun openSearch() = _state.update { it.copy(isSearchOpen = true, results = emptyList(), query = "") }
    fun closeSearch() = _state.update { it.copy(isSearchOpen = false) }

    fun updateQuery(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(results = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MILLIS)
            _state.update { it.copy(isSearching = true) }
            val results = searchService.search(query)
            _state.update { it.copy(results = results, isSearching = false) }
        }
    }

    fun selectDestination(result: SearchResult, currentPosition: RoutePoint) {
        _state.update { it.copy(isSearchOpen = false, isRouting = true, destinationName = result.displayName, error = null) }
        routeTo(result.point, currentPosition)
    }

    fun reroute(currentPosition: RoutePoint) {
        val destination = _state.value.activeRoute?.points?.lastOrNull() ?: return
        _state.update { it.copy(isRouting = true, error = null) }
        routeTo(destination, currentPosition)
    }

    private fun routeTo(destination: RoutePoint, origin: RoutePoint) {
        viewModelScope.launch {
            val outcome = navigationProvider.calculateRoute(RouteRequest(origin = origin, destination = destination))
            outcome.fold(
                onSuccess = { route -> _state.update { it.copy(isRouting = false, activeRoute = route, error = null) } },
                onFailure = { e -> _state.update { it.copy(isRouting = false, error = e.message ?: "Could not calculate a route") } },
            )
        }
    }

    fun cancelNavigation() {
        searchJob?.cancel()
        _state.value = RideNavigationUiState()
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 400L
    }
}
