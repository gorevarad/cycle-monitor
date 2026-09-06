package com.cyclemonitor.app.di

import android.content.Context
import com.cyclemonitor.app.BuildConfig
import com.cyclemonitor.app.data.db.CycleDatabase
import com.cyclemonitor.app.data.repository.DashboardProfileRepository
import com.cyclemonitor.app.data.repository.RoomRideRepository
import com.cyclemonitor.app.data.settings.DataRetention
import com.cyclemonitor.app.data.settings.SettingsRepository
import com.cyclemonitor.app.map.MapAvailability
import com.cyclemonitor.app.ride.location.FusedLocationProviderAdapter
import com.cyclemonitor.app.routing.DestinationSearchService
import com.cyclemonitor.app.routing.GoogleDirectionsNavigationProvider
import com.cyclemonitor.core.location.LocationAccuracyMode
import com.cyclemonitor.core.location.LocationProvider
import com.cyclemonitor.core.location.MockLocationProvider
import com.cyclemonitor.core.navigation.NavigationProvider
import com.cyclemonitor.core.navigation.StraightLineNavigationProvider
import com.cyclemonitor.core.power.PowerEstimationEngine
import com.cyclemonitor.core.power.PowerEstimator
import com.cyclemonitor.core.repository.RideRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * Hand-rolled composition root (no DI framework) so the dependency graph stays simple to read
 * and doesn't add another unverified build dependency. Every dependency is expressed behind a
 * `:core` interface (LocationProvider, PowerEstimator, RideRepository, NavigationProvider) so
 * tests/previews can substitute fakes without touching this class.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** Process-lifetime scope for one-off startup work (preset seeding, data-retention cleanup). */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val powerEstimator: PowerEstimator by lazy { PowerEstimationEngine() }

    /**
     * Real Directions-API cycling routing when a Maps key is configured (Directions API must be
     * enabled for that key in Google Cloud Console); otherwise a same-key-independent straight
     * line so the UI always has *something* honest to draw. See GoogleDirectionsNavigationProvider.
     */
    val navigationProvider: NavigationProvider by lazy {
        if (MapAvailability.isConfigured) {
            GoogleDirectionsNavigationProvider(MapAvailability.apiKey)
        } else {
            StraightLineNavigationProvider()
        }
    }

    val destinationSearchService: DestinationSearchService by lazy { DestinationSearchService(appContext) }

    private val database: CycleDatabase by lazy { CycleDatabase.build(appContext) }

    val rideRepository: RideRepository by lazy {
        RoomRideRepository(database.rideDao(), database.trackPointDao())
    }

    val dashboardProfileRepository: DashboardProfileRepository by lazy {
        DashboardProfileRepository(database.dashboardProfileDao())
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    init {
        applicationScope.launch {
            dashboardProfileRepository.seedPresetsIfEmpty()
            runDataRetentionCleanup()
        }
    }

    private suspend fun runDataRetentionCleanup() {
        val retention = settingsRepository.settings.first().dataRetention
        val days = retention.days ?: return
        val cutoff = Instant.now().minusSeconds(days.toLong() * 24 * 3600).toEpochMilli()
        rideRepository.deleteRidesOlderThan(cutoff)
    }

    /**
     * Real GPS in production. In a debug build, [useMock] can flip this to
     * [MockLocationProvider] (from Settings > Developer, never exposed in release) so the whole
     * dashboard/recording pipeline can be exercised without physically riding a bike -- see
     * BuildConfig.ALLOW_MOCK_LOCATION, which is hardcoded false in release builds so this branch
     * cannot compile-in mock data to a shipped app.
     */
    fun createLocationProvider(
        useMock: Boolean,
        accuracyMode: LocationAccuracyMode = LocationAccuracyMode.NORMAL,
    ): LocationProvider {
        return if (BuildConfig.ALLOW_MOCK_LOCATION && useMock) {
            MockLocationProvider()
        } else {
            FusedLocationProviderAdapter(appContext, accuracyMode)
        }
    }
}
