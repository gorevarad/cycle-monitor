package com.cyclemonitor.core.model

enum class BikeType(
    /** Baseline drag area (CdA, m^2) assumption used by [com.cyclemonitor.core.power.PowerEstimationEngine]. */
    val defaultCdA: Double,
    /** Baseline rolling resistance coefficient assumption. */
    val defaultCrr: Double,
) {
    ROAD(defaultCdA = 0.32, defaultCrr = 0.005),
    GRAVEL(defaultCdA = 0.36, defaultCrr = 0.008),
    MOUNTAIN(defaultCdA = 0.40, defaultCrr = 0.012),
    TIME_TRIAL(defaultCdA = 0.25, defaultCrr = 0.005),
    HYBRID(defaultCdA = 0.42, defaultCrr = 0.007),
    E_BIKE(defaultCdA = 0.40, defaultCrr = 0.007),
}

/** Riding position adjusts effective frontal area on top of [BikeType.defaultCdA]. */
enum class RidingPosition(val cdaMultiplier: Double) {
    TOPS(1.15),
    HOODS(1.0),
    DROPS(0.90),
    AERO_BARS(0.78),
}

/**
 * User-configurable parameters that feed the power estimation model. Defaults are reasonable
 * for an average adult rider on a road bike but must be editable in Settings, since the
 * estimate is only as good as these inputs.
 */
data class RiderProfile(
    val riderWeightKg: Double = 75.0,
    val bikeWeightKg: Double = 9.0,
    val bikeType: BikeType = BikeType.ROAD,
    val ridingPosition: RidingPosition = RidingPosition.HOODS,
    /** Drivetrain mechanical efficiency, e.g. 0.97 for a clean, well-lubricated chain. */
    val drivetrainEfficiency: Double = 0.97,
) {
    val totalMassKg: Double get() = riderWeightKg + bikeWeightKg

    val effectiveCdA: Double get() = bikeType.defaultCdA * ridingPosition.cdaMultiplier

    init {
        require(riderWeightKg in 20.0..250.0) { "riderWeightKg out of plausible range: $riderWeightKg" }
        require(bikeWeightKg in 3.0..60.0) { "bikeWeightKg out of plausible range: $bikeWeightKg" }
        require(drivetrainEfficiency in 0.7..1.0) { "drivetrainEfficiency out of plausible range: $drivetrainEfficiency" }
    }
}
