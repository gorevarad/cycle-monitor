package com.cyclemonitor.app.routing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyclemonitor.core.navigation.NavRoute
import com.cyclemonitor.core.navigation.NavigationProvider
import com.cyclemonitor.core.navigation.RouteProgress
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
    /** Live distance remaining along the route (see [RouteProgress]) -- null until the first
     * position update arrives after a route is calculated, in which case the banner falls back
     * to the route's total distance. */
    val remainingDistanceMeters: Double? = null,
    val isOffRoute: Boolean = false,
    val currentStepIndex: Int = 0,
) {
    val isNavigating: Boolean get() = activeRoute != null
    val currentStep get() = activeRoute?.steps?.getOrNull(currentStepIndex) ?: activeRoute?.steps?.firstOrNull()
}

/**
 * Drives ROUTE PLANNING for the ride screen: destination search, route calculation, live
 * progress along the route, and automatic re-routing when the rider drifts off it. Deliberately
 * separate from map display (see [NavigationProvider] doc) -- this class never touches
 * Compose/Maps types directly.
 */
class RideNavigationViewModel(
    private val navigationProvider: NavigationProvider,
    private val searchService: DestinationSearchService,
) : ViewModel() {

    private val _state = MutableStateFlow(RideNavigationUiState())
    val state: StateFlow<RideNavigationUiState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var offRouteSinceMillis: Long? = null

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
        offRouteSinceMillis = null
        _state.update { it.copy(isSearchOpen = false, isRouting = true, destinationName = result.displayName, error = null) }
        routeTo(result.point, currentPosition)
    }

    fun reroute(currentPosition: RoutePoint) {
        val destination = _state.value.activeRoute?.points?.lastOrNull() ?: return
        offRouteSinceMillis = null
        _state.update { it.copy(isRouting = true, error = null) }
        routeTo(destination, currentPosition)
    }

    /**
     * Called on every rider position update while navigating: refreshes the live
     * distance-remaining and, if the rider has been consistently off the calculated route for
     * longer than [OFF_ROUTE_REROUTE_DELAY_MILLIS], triggers one automatic re-route from here.
     */
    fun updateCurrentPosition(position: RoutePoint) {
        val route = _state.value.activeRoute ?: return
        val remaining = RouteProgress.remainingDistanceMeters(route.points, position.latitude, position.longitude)
        val offRoute = RouteProgress.isOffRoute(route.points, position.latitude, position.longitude)
        val traveled = remaining?.let { (route.distanceMeters - it).coerceAtLeast(0.0) }
        val stepIndex = traveled?.let { RouteProgress.currentStepIndex(route.steps, it) }
        _state.update {
            it.copy(
                remainingDistanceMeters = remaining ?: it.remainingDistanceMeters,
                isOffRoute = offRoute,
                currentStepIndex = stepIndex ?: it.currentStepIndex,
            )
        }

        if (!offRoute) {
            offRouteSinceMillis = null
            return
        }
        val since = offRouteSinceMillis ?: System.currentTimeMillis().also { offRouteSinceMillis = it }
        if (System.currentTimeMillis() - since < OFF_ROUTE_REROUTE_DELAY_MILLIS) return
        if (_state.value.isRouting) return

        offRouteSinceMillis = null
        reroute(position)
    }

    private fun routeTo(destination: RoutePoint, origin: RoutePoint) {
        viewModelScope.launch {
            val outcome = navigationProvider.calculateRoute(RouteRequest(origin = origin, destination = destination))
            outcome.fold(
                onSuccess = { route ->
                    _state.update {
                        it.copy(
                            isRouting = false,
                            activeRoute = route,
                            error = null,
                            remainingDistanceMeters = route.distanceMeters,
                            isOffRoute = false,
                            currentStepIndex = 0,
                        )
                    }
                },
                onFailure = { e -> _state.update { it.copy(isRouting = false, error = e.message ?: "Could not calculate a route") } },
            )
        }
    }

    fun cancelNavigation() {
        searchJob?.cancel()
        offRouteSinceMillis = null
        _state.value = RideNavigationUiState()
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 400L
        const val OFF_ROUTE_REROUTE_DELAY_MILLIS = 15_000L
    }
}
