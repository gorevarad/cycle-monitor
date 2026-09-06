package com.cyclemonitor.app.data.repository

import com.cyclemonitor.app.data.db.DashboardProfileDao
import com.cyclemonitor.app.data.db.DashboardProfileEntity
import com.cyclemonitor.core.model.AnimationIntensity
import com.cyclemonitor.core.model.DashboardMetric
import com.cyclemonitor.core.model.DashboardProfile
import com.cyclemonitor.core.model.OrientationBehavior
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val ROAD_ACCENT = 0xFF39D6E0.toInt()
private const val CLIMB_ACCENT = 0xFFE87A3A.toInt()
private const val RACE_ACCENT = 0xFFE84C4C.toInt()
private const val CASUAL_ACCENT = 0xFF4CD97B.toInt()

/** Selectable accent colors for custom profiles -- the app's semantic palette, not a full color picker. */
val CUSTOM_PROFILE_ACCENT_CHOICES = listOf(ROAD_ACCENT, CLIMB_ACCENT, RACE_ACCENT, CASUAL_ACCENT, 0xFFE8B23A.toInt())

/**
 * Persists dashboard profiles (built-in presets + user-created custom ones) in Room, seeding the
 * four presets on first run. Presets can be edited like any profile but [deleteProfile] refuses
 * to remove them (see [DashboardProfileDao.deleteCustom]).
 */
class DashboardProfileRepository(private val dao: DashboardProfileDao) {

    fun observeProfiles(): Flow<List<DashboardProfile>> = dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getProfile(id: String): DashboardProfile? = dao.getById(id)?.toDomain()

    /** Always creates a new, deletable row -- used when the user duplicates/creates a profile. */
    suspend fun createCustomProfile(profile: DashboardProfile) {
        dao.upsert(profile.toEntity(isCustom = true))
    }

    /** Edits an existing profile in place, preserving whether it's a (non-deletable) preset. */
    suspend fun updateProfile(profile: DashboardProfile) {
        val isCustom = dao.getById(profile.id)?.isCustom ?: true
        dao.upsert(profile.toEntity(isCustom))
    }

    suspend fun deleteProfile(id: String) {
        dao.deleteCustom(id)
    }

    suspend fun seedPresetsIfEmpty() {
        if (dao.count() > 0) return
        dao.insertAll(
            listOf(
                DashboardProfile.defaultRoadProfile("road", ROAD_ACCENT).toEntity(isCustom = false),
                DashboardProfile.climbProfile("climb", CLIMB_ACCENT).toEntity(isCustom = false),
                DashboardProfile.raceProfile("race", RACE_ACCENT).toEntity(isCustom = false),
                DashboardProfile.casualProfile("casual", CASUAL_ACCENT).toEntity(isCustom = false),
            ),
        )
    }
}

private fun DashboardProfileEntity.toDomain() = DashboardProfile(
    id = id,
    name = name,
    metrics = metricsCsv.split(",").filter { it.isNotBlank() }.mapNotNull { runCatching { DashboardMetric.valueOf(it) }.getOrNull() },
    speedGaugeMaxKmh = speedGaugeMaxKmh,
    powerGaugeMaxWatts = powerGaugeMaxWatts,
    accentColorArgb = accentColorArgb,
    orientationBehavior = runCatching { OrientationBehavior.valueOf(orientationBehavior) }.getOrDefault(OrientationBehavior.AUTO),
    animationIntensity = runCatching { AnimationIntensity.valueOf(animationIntensity) }.getOrDefault(AnimationIntensity.STANDARD),
)

private fun DashboardProfile.toEntity(isCustom: Boolean) = DashboardProfileEntity(
    id = id,
    name = name,
    metricsCsv = metrics.joinToString(",") { it.name },
    speedGaugeMaxKmh = speedGaugeMaxKmh,
    powerGaugeMaxWatts = powerGaugeMaxWatts,
    accentColorArgb = accentColorArgb,
    orientationBehavior = orientationBehavior.name,
    animationIntensity = animationIntensity.name,
    isCustom = isCustom,
)
