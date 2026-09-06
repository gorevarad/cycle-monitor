package com.cyclemonitor.core.smoothing

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DisplaySmootherTest {

    @Test
    fun `first sample is returned unsmoothed`() {
        val smoother = DisplaySmoother(timeConstantSeconds = 2.0)
        assertThat(smoother.update(0L, 31.1)).isEqualTo(31.1)
    }

    @Test
    fun `noisy raw sequence produces a smoothed sequence with less variance`() {
        val smoother = DisplaySmoother(timeConstantSeconds = 3.0)
        val raw = listOf(31.1, 31.8, 30.9, 32.0, 29.5, 33.2)
        var t = 0L
        val displayed = raw.map { value ->
            val result = smoother.update(t, value)
            t += 1000
            result
        }

        val rawRange = raw.max() - raw.min()
        val displayedRange = displayed.max() - displayed.min()
        assertThat(displayedRange).isLessThan(rawRange)
    }

    @Test
    fun `large gap lets the display value catch up faster than a small gap`() {
        val smootherLargeGap = DisplaySmoother(timeConstantSeconds = 3.0)
        smootherLargeGap.update(0L, 10.0)
        val afterLargeGap = smootherLargeGap.update(10_000L, 20.0)

        val smootherSmallGap = DisplaySmoother(timeConstantSeconds = 3.0)
        smootherSmallGap.update(0L, 10.0)
        val afterSmallGap = smootherSmallGap.update(1_000L, 20.0)

        assertThat(afterLargeGap).isGreaterThan(afterSmallGap)
    }

    @Test
    fun `zero or negative dt does not change the value`() {
        val smoother = DisplaySmoother(timeConstantSeconds = 2.0)
        smoother.update(1000L, 10.0)
        val result = smoother.update(1000L, 999.0)
        assertThat(result).isEqualTo(10.0)
    }

    @Test
    fun `reset clears state so the next sample is unsmoothed`() {
        val smoother = DisplaySmoother(timeConstantSeconds = 2.0)
        smoother.update(0L, 10.0)
        smoother.update(1000L, 20.0)
        smoother.reset()
        assertThat(smoother.update(5000L, 42.0)).isEqualTo(42.0)
    }
}

class TimeWindowAverageTest {

    @Test
    fun `average reflects only samples within the trailing window`() {
        val window = TimeWindowAverage(windowSeconds = 3.0)
        window.add(0L, 100.0)
        window.add(1000L, 200.0)
        window.add(2000L, 300.0)
        // This sample is > 3s after the first one, so it should fall out of the window.
        val avg = window.add(4000L, 400.0)
        assertThat(avg).isWithin(0.001).of((200.0 + 300.0 + 400.0) / 3.0)
    }

    @Test
    fun `empty window has null average`() {
        val window = TimeWindowAverage(windowSeconds = 3.0)
        assertThat(window.average).isNull()
    }
}

class CumulativeAverageTest {

    @Test
    fun `average of no samples is null`() {
        assertThat(CumulativeAverage().average).isNull()
    }

    @Test
    fun `average accumulates correctly`() {
        val avg = CumulativeAverage()
        avg.add(10.0)
        avg.add(20.0)
        avg.add(30.0)
        assertThat(avg.average).isWithin(0.001).of(20.0)
    }
}
