package com.cyclemonitor.core.model

enum class DashboardMetric {
    SPEED,
    AVERAGE_SPEED,
    MAX_SPEED,
    DISTANCE,
    ELAPSED_TIME,
    MOVING_TIME,
    ELEVATION_GAIN,
    GRADE,
    ESTIMATED_POWER,
    ESTIMATED_POWER_3S_AVG,
    ESTIMATED_POWER_AVG,
    ESTIMATED_POWER_MAX,
    GPS_STATUS,
    MAP,
}

enum class OrientationBehavior {
    AUTO,
    LANDSCAPE_LOCK,
    PORTRAIT_LOCK,
}

enum class AnimationIntensity {
    OFF,
    MINIMAL,
    STANDARD,
    DYNAMIC,
}

/**
 * A named, user-editable arrangement of the ride dashboard. Ships with presets (Road, Climb,
 * Race, Casual) but is fully customizable and persisted so riders can switch between them.
 */
data class DashboardProfile(
    val id: String,
    val name: String,
    val metrics: List<DashboardMetric>,
    val speedGaugeMaxKmh: Int = 60,
    val powerGaugeMaxWatts: Int = 400,
    val accentColorArgb: Int,
    val orientationBehavior: OrientationBehavior = OrientationBehavior.AUTO,
    val animationIntensity: AnimationIntensity = AnimationIntensity.STANDARD,
) {
    companion object {
        fun defaultRoadProfile(id: String, accentColorArgb: Int): DashboardProfile = DashboardProfile(
            id = id,
            name = "Road",
            metrics = listOf(
                DashboardMetric.SPEED,
                DashboardMetric.MAP,
                DashboardMetric.ESTIMATED_POWER,
                DashboardMetric.DISTANCE,
                DashboardMetric.ELAPSED_TIME,
                DashboardMetric.ELEVATION_GAIN,
                DashboardMetric.GRADE,
                DashboardMetric.GPS_STATUS,
            ),
            accentColorArgb = accentColorArgb,
        )

        fun climbProfile(id: String, accentColorArgb: Int): DashboardProfile = DashboardProfile(
            id = id,
            name = "Climb",
            metrics = listOf(
                DashboardMetric.GRADE,
                DashboardMetric.ELEVATION_GAIN,
                DashboardMetric.ESTIMATED_POWER,
                DashboardMetric.SPEED,
            ),
            speedGaugeMaxKmh = 40,
            accentColorArgb = accentColorArgb,
        )

        fun raceProfile(id: String, accentColorArgb: Int): DashboardProfile = DashboardProfile(
            id = id,
            name = "Race",
            metrics = listOf(
                DashboardMetric.SPEED,
                DashboardMetric.ESTIMATED_POWER_3S_AVG,
                DashboardMetric.DISTANCE,
                DashboardMetric.ELAPSED_TIME,
            ),
            speedGaugeMaxKmh = 80,
            animationIntensity = AnimationIntensity.DYNAMIC,
            accentColorArgb = accentColorArgb,
        )

        fun casualProfile(id: String, accentColorArgb: Int): DashboardProfile = DashboardProfile(
            id = id,
            name = "Casual",
            metrics = listOf(
                DashboardMetric.SPEED,
                DashboardMetric.DISTANCE,
                DashboardMetric.MAP,
                DashboardMetric.ELAPSED_TIME,
            ),
            animationIntensity = AnimationIntensity.MINIMAL,
            accentColorArgb = accentColorArgb,
        )
    }
}
