package com.cyclemonitor.app.di

import android.content.Context
import com.cyclemonitor.app.BuildConfig
import com.cyclemonitor.app.data.db.CycleDatabase
import com.cyclemonitor.app.data.repository.RoomRideRepository
import com.cyclemonitor.app.data.settings.SettingsRepository
import com.cyclemonitor.app.ride.location.FusedLocationProviderAdapter
import com.cyclemonitor.core.location.LocationAccuracyMode
import com.cyclemonitor.core.location.LocationProvider
import com.cyclemonitor.core.location.MockLocationProvider
import com.cyclemonitor.core.navigation.NavigationProvider
import com.cyclemonitor.core.navigation.StraightLineNavigationProvider
import com.cyclemonitor.core.power.PowerEstimationEngine
import com.cyclemonitor.core.power.PowerEstimator
import com.cyclemonitor.core.repository.RideRepository

/**
 * Hand-rolled composition root (no DI framework) so the dependency graph stays simple to read
 * and doesn't add another unverified build dependency. Every dependency is expressed behind a
 * `:core` interface (LocationProvider, PowerEstimator, RideRepository, NavigationProvider) so
 * tests/previews can substitute fakes without touching this class.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val powerEstimator: PowerEstimator by lazy { PowerEstimationEngine() }

    val navigationProvider: NavigationProvider by lazy { StraightLineNavigationProvider() }

    private val database: CycleDatabase by lazy { CycleDatabase.build(appContext) }

    val rideRepository: RideRepository by lazy {
        RoomRideRepository(database.rideDao(), database.trackPointDao())
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

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
