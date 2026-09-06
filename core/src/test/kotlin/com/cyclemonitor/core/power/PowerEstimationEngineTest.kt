package com.cyclemonitor.core.power

import com.cyclemonitor.core.model.RiderProfile
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PowerEstimationEngineTest {

    private val engine = PowerEstimationEngine()
    private val profile = RiderProfile()

    private fun input(
        speedMps: Double,
        previousSpeedMps: Double? = speedMps,
        dtSeconds: Double? = 1.0,
        gradePercent: Double? = 0.0,
        gpsAccuracyMeters: Float? = 4f,
    ) = PowerEstimationInput(
        speedMps = speedMps,
        previousSpeedMps = previousSpeedMps,
        dtSeconds = dtSeconds,
        gradePercent = gradePercent,
        gpsAccuracyMeters = gpsAccuracyMeters,
        riderProfile = profile,
    )

    @Test
    fun `stopped rider produces zero watts with high confidence`() {
        val estimate = engine.estimate(input(speedMps = 0.0))
        assertThat(estimate.rawWatts).isEqualTo(0.0)
        assertThat(estimate.confidence).isEqualTo(EstimationConfidence.HIGH)
    }

    @Test
    fun `steady flat riding produces a positive plausible wattage`() {
        // ~28.8 km/h steady state on the flat is a realistic few-hundred-watt-ish output
        // for the default 75kg rider + 9kg bike profile.
        val estimate = engine.estimate(input(speedMps = 8.0, gradePercent = 0.0))
        assertThat(estimate.rawWatts).isNotNull()
        assertThat(estimate.rawWatts!!).isGreaterThan(0.0)
        assertThat(estimate.rawWatts!!).isLessThan(1000.0)
        assertThat(estimate.confidence).isEqualTo(EstimationConfidence.HIGH)
    }

    @Test
    fun `climbing requires more power than flat at the same speed`() {
        val flat = engine.estimate(input(speedMps = 6.0, gradePercent = 0.0))
        val climb = engine.estimate(input(speedMps = 6.0, gradePercent = 8.0))
        assertThat(climb.rawWatts!!).isGreaterThan(flat.rawWatts!!)
    }

    @Test
    fun `accelerating requires more power than steady state`() {
        val steady = engine.estimate(input(speedMps = 6.0, previousSpeedMps = 6.0))
        val accelerating = engine.estimate(input(speedMps = 8.0, previousSpeedMps = 4.0))
        assertThat(accelerating.rawWatts!!).isGreaterThan(steady.rawWatts!!)
    }

    @Test
    fun `steep descent yields zero watts and flags coasting`() {
        val estimate = engine.estimate(input(speedMps = 12.0, gradePercent = -10.0))
        assertThat(estimate.rawWatts).isEqualTo(0.0)
        assertThat(estimate.reasons).contains(LowConfidenceReason.COASTING_OR_DESCENDING)
    }

    @Test
    fun `missing elevation lowers confidence and is flagged`() {
        val estimate = engine.estimate(input(speedMps = 8.0, gradePercent = null))
        assertThat(estimate.reasons).contains(LowConfidenceReason.MISSING_ELEVATION)
        assertThat(estimate.confidence).isNotEqualTo(EstimationConfidence.HIGH)
    }

    @Test
    fun `weak gps accuracy lowers confidence and is flagged`() {
        val estimate = engine.estimate(input(speedMps = 8.0, gpsAccuracyMeters = 45f))
        assertThat(estimate.reasons).contains(LowConfidenceReason.WEAK_GPS_ACCURACY)
        assertThat(estimate.confidence).isNotEqualTo(EstimationConfidence.HIGH)
    }

    @Test
    fun `missing gps accuracy is treated as weak`() {
        val estimate = engine.estimate(input(speedMps = 8.0, gpsAccuracyMeters = null))
        assertThat(estimate.reasons).contains(LowConfidenceReason.WEAK_GPS_ACCURACY)
    }

    @Test
    fun `implausible gps speed jump is clamped and flagged rather than scaling with the raw jump`() {
        // A sudden 8 m/s^2 "acceleration" between samples is a GPS glitch, not a real cyclist.
        val estimate = engine.estimate(input(speedMps = 10.0, previousSpeedMps = 2.0, dtSeconds = 1.0))
        assertThat(estimate.reasons).contains(LowConfidenceReason.GPS_SPEED_JITTER)

        // Compare against what the unclamped acceleration would have produced: the clamped
        // estimate must be meaningfully smaller, proving the clamp actually took effect.
        val steadyStateAtSameSpeed = engine.estimate(input(speedMps = 10.0, previousSpeedMps = 10.0, dtSeconds = 1.0))
        val unclampedAccelerationForce = profile.totalMassKg * 8.0
        val unclampedExtraPower = unclampedAccelerationForce * 10.0 / profile.drivetrainEfficiency
        val unclampedEstimate = steadyStateAtSameSpeed.rawWatts!! + unclampedExtraPower

        assertThat(estimate.rawWatts).isNotNull()
        assertThat(estimate.rawWatts!!).isLessThan(unclampedEstimate)
    }

    @Test
    fun `null dt is unavailable rather than a guess`() {
        val estimate = engine.estimate(input(speedMps = 8.0, dtSeconds = null))
        assertThat(estimate.rawWatts).isNull()
        assertThat(estimate.confidence).isEqualTo(EstimationConfidence.UNAVAILABLE)
    }

    @Test
    fun `zero or negative dt is unavailable`() {
        val estimate = engine.estimate(input(speedMps = 8.0, dtSeconds = 0.0))
        assertThat(estimate.confidence).isEqualTo(EstimationConfidence.UNAVAILABLE)
    }

    @Test
    fun `very large time gap is unavailable rather than integrating over the gap`() {
        val estimate = engine.estimate(input(speedMps = 8.0, dtSeconds = 60.0))
        assertThat(estimate.rawWatts).isNull()
        assertThat(estimate.confidence).isEqualTo(EstimationConfidence.UNAVAILABLE)
        assertThat(estimate.reasons).contains(LowConfidenceReason.LARGE_TIME_GAP)
    }

    @Test
    fun `moderate time gap degrades confidence but still produces a number`() {
        val estimate = engine.estimate(input(speedMps = 8.0, dtSeconds = 7.0))
        assertThat(estimate.rawWatts).isNotNull()
        assertThat(estimate.reasons).contains(LowConfidenceReason.LARGE_TIME_GAP)
    }

    @Test
    fun `heavier rider requires more power at the same speed and grade`() {
        val lightEngine = engine.estimate(input(speedMps = 8.0).copy(riderProfile = RiderProfile(riderWeightKg = 60.0)))
        val heavyEngine = engine.estimate(input(speedMps = 8.0).copy(riderProfile = RiderProfile(riderWeightKg = 100.0)))
        assertThat(heavyEngine.rawWatts!!).isGreaterThan(lightEngine.rawWatts!!)
    }

    @Test
    fun `multiple degraded inputs stack to LOW confidence`() {
        val estimate = engine.estimate(
            input(speedMps = 8.0, gradePercent = null, gpsAccuracyMeters = 45f),
        )
        assertThat(estimate.confidence).isEqualTo(EstimationConfidence.LOW)
    }
}
