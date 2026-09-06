package com.cyclemonitor.core.smoothing

/**
 * Time-aware exponential smoother for noisy GPS-derived instantaneous values (speed, power)
 * intended purely for *display*. It never touches the raw recorded data — callers keep raw
 * samples separately and only feed values through this class to decide what number to render.
 *
 * Unlike a fixed-alpha EMA, this accounts for irregular GPS sample intervals: a longer gap
 * between updates lets the smoothed value catch up faster, so the display does not lag behind
 * reality when updates are sparse.
 *
 * @param timeConstantSeconds larger = smoother but slower to react. ~1.5-2.5s suits a speed
 *   gauge; power benefits from a longer constant since the underlying estimate is noisier.
 */
class DisplaySmoother(private val timeConstantSeconds: Double) {
    init {
        require(timeConstantSeconds > 0.0) { "timeConstantSeconds must be positive" }
    }

    var currentValue: Double? = null
        private set

    private var lastTimestampMillis: Long? = null

    fun reset() {
        currentValue = null
        lastTimestampMillis = null
    }

    /** Feeds a new raw sample at [timestampMillis] and returns the updated smoothed value. */
    fun update(timestampMillis: Long, rawValue: Double): Double {
        val previousValue = currentValue
        val previousTimestamp = lastTimestampMillis
        lastTimestampMillis = timestampMillis

        if (previousValue == null || previousTimestamp == null) {
            currentValue = rawValue
            return rawValue
        }

        val dtSeconds = (timestampMillis - previousTimestamp).coerceAtLeast(0L) / 1000.0
        if (dtSeconds <= 0.0) {
            return previousValue
        }

        // Standard continuous-time exponential smoothing: alpha derived from dt/tau so the
        // effective smoothing strength stays consistent even with variable GPS update rates.
        val alpha = 1.0 - kotlin.math.exp(-dtSeconds / timeConstantSeconds)
        val smoothed = previousValue + alpha * (rawValue - previousValue)
        currentValue = smoothed
        return smoothed
    }
}

/**
 * Rolling average of raw samples within a fixed trailing time window (e.g. "3s average power").
 * Distinct from [DisplaySmoother]: this is a true windowed average rather than an exponential
 * decay, matching how cycling computers usually define "Xs average" fields.
 */
class TimeWindowAverage(private val windowSeconds: Double) {
    init {
        require(windowSeconds > 0.0) { "windowSeconds must be positive" }
    }

    private val samples = ArrayDeque<Pair<Long, Double>>()

    val average: Double?
        get() = if (samples.isEmpty()) null else samples.sumOf { it.second } / samples.size

    fun add(timestampMillis: Long, value: Double): Double? {
        samples.addLast(timestampMillis to value)
        val cutoff = timestampMillis - (windowSeconds * 1000).toLong()
        while (samples.isNotEmpty() && samples.first().first < cutoff) {
            samples.removeFirst()
        }
        return average
    }

    fun reset() = samples.clear()
}

/** Simple cumulative (all-samples) average, e.g. for whole-ride average speed/power. */
class CumulativeAverage {
    private var count: Long = 0
    private var total: Double = 0.0

    val average: Double? get() = if (count == 0L) null else total / count

    fun add(value: Double) {
        total += value
        count += 1
    }
}
