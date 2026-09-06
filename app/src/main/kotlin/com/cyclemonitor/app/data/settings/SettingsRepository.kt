package com.cyclemonitor.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cyclemonitor.core.location.LocationAccuracyMode
import com.cyclemonitor.core.model.AnimationIntensity
import com.cyclemonitor.core.model.BikeType
import com.cyclemonitor.core.model.OrientationBehavior
import com.cyclemonitor.core.model.RidingPosition
import com.cyclemonitor.core.model.RiderProfile
import com.cyclemonitor.core.units.DistanceUnit
import com.cyclemonitor.core.units.ElevationUnit
import com.cyclemonitor.core.units.SpeedUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "cycle_monitor_settings")

enum class ThemePreference { DARK, LIGHT, SYSTEM }

enum class DataRetention(val days: Int?, val label: String) {
    ONE_MONTH(30, "1 month"),
    SIX_MONTHS(182, "6 months"),
    ONE_YEAR(365, "1 year"),
    FOREVER(null, "Keep forever"),
}

enum class MapStyle { STANDARD, SATELLITE, TERRAIN, NIGHT }

/** Every user-configurable, persisted, app-wide setting. Ride-specific dashboard layout choice
 * lives here too (as a preset id) rather than as a fully custom profile editor -- see README for
 * why that scope was cut from this pass. */
data class UserSettings(
    val riderProfile: RiderProfile = RiderProfile(),
    val speedUnit: SpeedUnit = SpeedUnit.KMH,
    val distanceUnit: DistanceUnit = DistanceUnit.KILOMETERS,
    val elevationUnit: ElevationUnit = ElevationUnit.METERS,
    val theme: ThemePreference = ThemePreference.DARK,
    val orientationBehavior: OrientationBehavior = OrientationBehavior.AUTO,
    val animationIntensity: AnimationIntensity = AnimationIntensity.STANDARD,
    val locationAccuracyMode: LocationAccuracyMode = LocationAccuracyMode.NORMAL,
    val autoPauseEnabled: Boolean = true,
    val dashboardProfileId: String = "road",
    val useMockLocationForDevelopment: Boolean = false,
    val dataRetention: DataRetention = DataRetention.FOREVER,
    val mapStyle: MapStyle = MapStyle.STANDARD,
)

class SettingsRepository(private val context: Context) {

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            riderProfile = RiderProfile(
                riderWeightKg = prefs[Keys.RIDER_WEIGHT_KG] ?: 75.0,
                bikeWeightKg = prefs[Keys.BIKE_WEIGHT_KG] ?: 9.0,
                bikeType = prefs[Keys.BIKE_TYPE]?.let { runCatching { BikeType.valueOf(it) }.getOrNull() } ?: BikeType.ROAD,
                ridingPosition = prefs[Keys.RIDING_POSITION]?.let { runCatching { RidingPosition.valueOf(it) }.getOrNull() } ?: RidingPosition.HOODS,
                drivetrainEfficiency = prefs[Keys.DRIVETRAIN_EFFICIENCY] ?: 0.97,
            ),
            speedUnit = prefs[Keys.SPEED_UNIT]?.let { runCatching { SpeedUnit.valueOf(it) }.getOrNull() } ?: SpeedUnit.KMH,
            distanceUnit = prefs[Keys.DISTANCE_UNIT]?.let { runCatching { DistanceUnit.valueOf(it) }.getOrNull() } ?: DistanceUnit.KILOMETERS,
            elevationUnit = prefs[Keys.ELEVATION_UNIT]?.let { runCatching { ElevationUnit.valueOf(it) }.getOrNull() } ?: ElevationUnit.METERS,
            theme = prefs[Keys.THEME]?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() } ?: ThemePreference.DARK,
            orientationBehavior = prefs[Keys.ORIENTATION_BEHAVIOR]?.let { runCatching { OrientationBehavior.valueOf(it) }.getOrNull() } ?: OrientationBehavior.AUTO,
            animationIntensity = prefs[Keys.ANIMATION_INTENSITY]?.let { runCatching { AnimationIntensity.valueOf(it) }.getOrNull() } ?: AnimationIntensity.STANDARD,
            locationAccuracyMode = prefs[Keys.LOCATION_ACCURACY_MODE]?.let { runCatching { LocationAccuracyMode.valueOf(it) }.getOrNull() } ?: LocationAccuracyMode.NORMAL,
            autoPauseEnabled = prefs[Keys.AUTO_PAUSE_ENABLED] ?: true,
            dashboardProfileId = prefs[Keys.DASHBOARD_PROFILE_ID] ?: "road",
            useMockLocationForDevelopment = prefs[Keys.USE_MOCK_LOCATION] ?: false,
            dataRetention = prefs[Keys.DATA_RETENTION]?.let { runCatching { DataRetention.valueOf(it) }.getOrNull() } ?: DataRetention.FOREVER,
            mapStyle = prefs[Keys.MAP_STYLE]?.let { runCatching { MapStyle.valueOf(it) }.getOrNull() } ?: MapStyle.STANDARD,
        )
    }

    suspend fun updateRiderProfile(profile: RiderProfile) {
        context.dataStore.edit { prefs ->
            prefs[Keys.RIDER_WEIGHT_KG] = profile.riderWeightKg
            prefs[Keys.BIKE_WEIGHT_KG] = profile.bikeWeightKg
            prefs[Keys.BIKE_TYPE] = profile.bikeType.name
            prefs[Keys.RIDING_POSITION] = profile.ridingPosition.name
            prefs[Keys.DRIVETRAIN_EFFICIENCY] = profile.drivetrainEfficiency
        }
    }

    suspend fun updateSpeedUnit(unit: SpeedUnit) = context.dataStore.edit { it[Keys.SPEED_UNIT] = unit.name }
    suspend fun updateDistanceUnit(unit: DistanceUnit) = context.dataStore.edit { it[Keys.DISTANCE_UNIT] = unit.name }
    suspend fun updateElevationUnit(unit: ElevationUnit) = context.dataStore.edit { it[Keys.ELEVATION_UNIT] = unit.name }
    suspend fun updateTheme(theme: ThemePreference) = context.dataStore.edit { it[Keys.THEME] = theme.name }
    suspend fun updateOrientationBehavior(behavior: OrientationBehavior) = context.dataStore.edit { it[Keys.ORIENTATION_BEHAVIOR] = behavior.name }
    suspend fun updateAnimationIntensity(intensity: AnimationIntensity) = context.dataStore.edit { it[Keys.ANIMATION_INTENSITY] = intensity.name }
    suspend fun updateLocationAccuracyMode(mode: LocationAccuracyMode) = context.dataStore.edit { it[Keys.LOCATION_ACCURACY_MODE] = mode.name }
    suspend fun updateAutoPauseEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.AUTO_PAUSE_ENABLED] = enabled }
    suspend fun updateDashboardProfileId(id: String) = context.dataStore.edit { it[Keys.DASHBOARD_PROFILE_ID] = id }
    suspend fun updateUseMockLocationForDevelopment(enabled: Boolean) = context.dataStore.edit { it[Keys.USE_MOCK_LOCATION] = enabled }
    suspend fun updateDataRetention(retention: DataRetention) = context.dataStore.edit { it[Keys.DATA_RETENTION] = retention.name }
    suspend fun updateMapStyle(style: MapStyle) = context.dataStore.edit { it[Keys.MAP_STYLE] = style.name }

    private object Keys {
        val RIDER_WEIGHT_KG = doublePreferencesKey("rider_weight_kg")
        val BIKE_WEIGHT_KG = doublePreferencesKey("bike_weight_kg")
        val BIKE_TYPE = stringPreferencesKey("bike_type")
        val RIDING_POSITION = stringPreferencesKey("riding_position")
        val DRIVETRAIN_EFFICIENCY = doublePreferencesKey("drivetrain_efficiency")
        val SPEED_UNIT = stringPreferencesKey("speed_unit")
        val DISTANCE_UNIT = stringPreferencesKey("distance_unit")
        val ELEVATION_UNIT = stringPreferencesKey("elevation_unit")
        val THEME = stringPreferencesKey("theme")
        val ORIENTATION_BEHAVIOR = stringPreferencesKey("orientation_behavior")
        val ANIMATION_INTENSITY = stringPreferencesKey("animation_intensity")
        val LOCATION_ACCURACY_MODE = stringPreferencesKey("location_accuracy_mode")
        val AUTO_PAUSE_ENABLED = booleanPreferencesKey("auto_pause_enabled")
        val DASHBOARD_PROFILE_ID = stringPreferencesKey("dashboard_profile_id")
        val USE_MOCK_LOCATION = booleanPreferencesKey("use_mock_location")
        val DATA_RETENTION = stringPreferencesKey("data_retention")
        val MAP_STYLE = stringPreferencesKey("map_style")
    }
}
