package com.cyclemonitor.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GpsQualityTest {

    @Test
    fun `null accuracy is unavailable`() {
        assertThat(GpsQuality.classify(null)).isEqualTo(GpsQuality.UNAVAILABLE)
    }

    @Test
    fun `NaN accuracy is unavailable`() {
        assertThat(GpsQuality.classify(Float.NaN)).isEqualTo(GpsQuality.UNAVAILABLE)
    }

    @Test
    fun `tight accuracy is good`() {
        assertThat(GpsQuality.classify(3f)).isEqualTo(GpsQuality.GOOD)
        assertThat(GpsQuality.classify(8f)).isEqualTo(GpsQuality.GOOD)
    }

    @Test
    fun `moderate accuracy is moderate`() {
        assertThat(GpsQuality.classify(15f)).isEqualTo(GpsQuality.MODERATE)
        assertThat(GpsQuality.classify(20f)).isEqualTo(GpsQuality.MODERATE)
    }

    @Test
    fun `loose accuracy is weak`() {
        assertThat(GpsQuality.classify(28f)).isEqualTo(GpsQuality.WEAK)
        assertThat(GpsQuality.classify(500f)).isEqualTo(GpsQuality.WEAK)
    }
}

class RiderProfileTest {

    @Test
    fun `default profile is within plausible ranges`() {
        val profile = RiderProfile()
        assertThat(profile.totalMassKg).isGreaterThan(0.0)
        assertThat(profile.effectiveCdA).isGreaterThan(0.0)
    }

    @Test
    fun `implausible rider weight is rejected`() {
        try {
            RiderProfile(riderWeightKg = 5.0)
            throw AssertionError("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun `riding position changes effective drag area`() {
        val hoods = RiderProfile(ridingPosition = RidingPosition.HOODS)
        val drops = RiderProfile(ridingPosition = RidingPosition.DROPS)
        assertThat(drops.effectiveCdA).isLessThan(hoods.effectiveCdA)
    }
}
