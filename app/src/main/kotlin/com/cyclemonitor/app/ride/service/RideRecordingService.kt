package com.cyclemonitor.app.ride.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.cyclemonitor.app.CycleMonitorApplication
import com.cyclemonitor.app.MainActivity
import com.cyclemonitor.app.R
import com.cyclemonitor.core.location.LocationProvider
import com.cyclemonitor.core.model.RideDetail
import com.cyclemonitor.core.model.RideSummary
import com.cyclemonitor.core.model.TrackPoint
import com.cyclemonitor.core.ride.RideEngine
import com.cyclemonitor.core.ride.RideLiveStats
import com.cyclemonitor.core.ride.RideState
import com.cyclemonitor.core.ride.RideStateMachine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Foreground service that owns the live ride: it is the single source of truth for ride state
 * while a ride is active, independent of whatever the UI is doing. The dashboard/map can crash,
 * rotate, or be backgrounded without interrupting recording; only this service and [RideEngine]
 * need to be alive.
 *
 * Only one ride can be active at a time, so ride state is exposed as process-wide [StateFlow]s
 * rather than through a bound-service interface -- simpler, and sufficient for a single-rider
 * cycling computer.
 */
class RideRecordingService : LifecycleService() {

    private lateinit var locationProvider: LocationProvider
    private var rideEngine: RideEngine? = null
    private var locationJob: Job? = null
    private var checkpointJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var rideId: String = ""
    private var rideName: String = ""
    private var startTimeMillis: Long = 0L
    private val trackPoints = mutableListOf<TrackPoint>()

    private var isAutoPaused = false
    private var belowThresholdSinceMillis: Long? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> startRide()
            ACTION_PAUSE -> pauseRide(auto = false)
            ACTION_RESUME -> resumeRide(auto = false)
            ACTION_FINISH -> finishRide()
        }
        return START_STICKY
    }

    private fun startRide() {
        if (_rideState.value != RideState.Idle && _rideState.value !is RideState.Error) return
        _rideState.value = RideStateMachine.transition(_rideState.value, RideState.Starting)

        val app = application as CycleMonitorApplication
        rideId = UUID.randomUUID().toString()
        rideName = "Ride"
        startTimeMillis = System.currentTimeMillis()
        trackPoints.clear()
        isAutoPaused = false
        belowThresholdSinceMillis = null

        // Android requires startForeground() to follow startForegroundService() almost
        // immediately, so this happens synchronously with a generic message rather than waiting
        // on the settings read below.
        startForeground(NOTIFICATION_ID, buildNotification("Starting..."))
        acquireWakeLock()

        lifecycleScope.launch {
            // Rider/bike parameters and accuracy mode are captured once at ride start; changing
            // them mid-ride would retroactively skew a physics model that assumes a fixed mass,
            // so Settings changes take effect on the *next* ride rather than live-patching this one.
            val settings = app.container.settingsRepository.settings.first()

            rideEngine = RideEngine(riderProfile = settings.riderProfile, powerEstimator = app.container.powerEstimator)
            locationProvider = app.container.createLocationProvider(
                useMock = settings.useMockLocationForDevelopment,
                accuracyMode = settings.locationAccuracyMode,
            )

            _rideState.value = RideStateMachine.transition(_rideState.value, RideState.Riding)

            locationJob = lifecycleScope.launch {
                locationProvider.locationUpdates().collect { sample ->
                    val engine = rideEngine ?: return@collect
                    val result = engine.onLocationSample(sample) ?: return@collect
                    trackPoints += result.trackPoint
                    _liveStats.value = result.liveStats
                    _currentSample.value = sample

                    if (settings.autoPauseEnabled) {
                        handleAutoPause(result.liveStats, sample.timestampMillis)
                    }
                }
            }

            checkpointJob = lifecycleScope.launch {
                while (true) {
                    delay(CHECKPOINT_INTERVAL_MILLIS)
                    checkpointToDatabase(app)
                    _liveStats.value?.let { updateNotification(it) }
                }
            }
        }
    }

    private fun handleAutoPause(stats: RideLiveStats, timestampMillis: Long) {
        when {
            _rideState.value == RideState.Riding && stats.currentSpeedMps < AUTO_PAUSE_SPEED_THRESHOLD_MPS -> {
                val since = belowThresholdSinceMillis ?: timestampMillis.also { belowThresholdSinceMillis = it }
                if (timestampMillis - since > AUTO_PAUSE_DELAY_MILLIS) {
                    isAutoPaused = true
                    pauseRide(auto = true)
                }
            }
            _rideState.value == RideState.Riding -> belowThresholdSinceMillis = null
            _rideState.value == RideState.Paused && isAutoPaused && stats.currentSpeedMps > AUTO_RESUME_SPEED_THRESHOLD_MPS -> {
                isAutoPaused = false
                resumeRide(auto = true)
            }
        }
    }

    private fun pauseRide(auto: Boolean) {
        if (_rideState.value != RideState.Riding) return
        rideEngine?.pause()
        _rideState.value = RideStateMachine.transition(_rideState.value, RideState.Paused)
        updateNotification(_liveStats.value, suffix = if (auto) " (auto-paused)" else " (paused)")
    }

    private fun resumeRide(auto: Boolean) {
        if (_rideState.value != RideState.Paused) return
        if (!auto) isAutoPaused = false
        rideEngine?.resume()
        _rideState.value = RideStateMachine.transition(_rideState.value, RideState.Riding)
        belowThresholdSinceMillis = null
    }

    private fun finishRide() {
        if (_rideState.value != RideState.Riding && _rideState.value != RideState.Paused) return
        _rideState.value = RideStateMachine.transition(_rideState.value, RideState.Finishing)

        lifecycleScope.launch {
            checkpointToDatabase(application as CycleMonitorApplication, final = true)
            _rideState.value = RideStateMachine.transition(_rideState.value, RideState.Completed)
            lastCompletedRideId.value = rideId

            locationJob?.cancel()
            checkpointJob?.cancel()
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()

            // Reset to Idle so a new ride can be started; the just-finished ride's id remains
            // available via lastCompletedRideId for the UI to navigate to its summary.
            _rideState.value = RideStateMachine.transition(RideState.Completed, RideState.Idle)
            _liveStats.value = null
            _currentSample.value = null
        }
    }

    private suspend fun checkpointToDatabase(app: CycleMonitorApplication, final: Boolean = false) {
        if (trackPoints.isEmpty()) return
        val stats = _liveStats.value
        val summary = RideSummary(
            id = rideId,
            name = rideName,
            startTimeMillis = startTimeMillis,
            endTimeMillis = if (final) System.currentTimeMillis() else trackPoints.last().sample.timestampMillis,
            distanceMeters = stats?.distanceMeters ?: 0.0,
            movingTimeSeconds = stats?.movingTimeSeconds ?: 0L,
            totalTimeSeconds = stats?.totalTimeSeconds ?: 0L,
            averageSpeedMps = stats?.averageSpeedMps,
            maxSpeedMps = stats?.maxSpeedMps,
            elevationGainMeters = stats?.elevationGainMeters,
            maxElevationMeters = trackPoints.mapNotNull { it.sample.altitudeMeters }.maxOrNull(),
            averageGradePercent = trackPoints.mapNotNull { it.gradePercent }.average().takeIf { !it.isNaN() },
            maxGradePercent = trackPoints.mapNotNull { it.gradePercent }.maxOrNull(),
            estimatedAveragePowerWatts = stats?.averagePowerWatts,
            estimatedMaxPowerWatts = stats?.maxPowerWatts,
        )
        app.container.rideRepository.saveRide(RideDetail(summary = summary, trackPoints = trackPoints.toList()))
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService<PowerManager>() ?: return
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "CycleMonitor:RideRecording").apply {
            setReferenceCounted(false)
            acquire(MAX_WAKE_LOCK_DURATION_MILLIS)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun buildNotification(statusText: String) = NotificationCompat.Builder(this, CycleMonitorApplication.RIDE_RECORDING_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(getString(R.string.notification_ride_recording_title))
        .setContentText(statusText)
        .setOngoing(true)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
        .build()

    private fun updateNotification(stats: RideLiveStats?, suffix: String = "") {
        val text = if (stats == null) {
            "Recording$suffix"
        } else {
            val km = stats.distanceMeters / 1000.0
            val minutes = stats.movingTimeSeconds / 60
            val seconds = stats.movingTimeSeconds % 60
            "%.1f km · %d:%02d$suffix".format(km, minutes, seconds)
        }
        getSystemService<android.app.NotificationManager>()?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseWakeLock()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHECKPOINT_INTERVAL_MILLIS = 15_000L
        private const val AUTO_PAUSE_SPEED_THRESHOLD_MPS = 0.5
        private const val AUTO_RESUME_SPEED_THRESHOLD_MPS = 1.5
        private const val AUTO_PAUSE_DELAY_MILLIS = 8_000L
        private const val MAX_WAKE_LOCK_DURATION_MILLIS = 6 * 60 * 60 * 1000L // 6h safety cap

        const val ACTION_START = "com.cyclemonitor.app.action.START_RIDE"
        const val ACTION_PAUSE = "com.cyclemonitor.app.action.PAUSE_RIDE"
        const val ACTION_RESUME = "com.cyclemonitor.app.action.RESUME_RIDE"
        const val ACTION_FINISH = "com.cyclemonitor.app.action.FINISH_RIDE"

        private val _rideState = MutableStateFlow<RideState>(RideState.Idle)
        val rideState = _rideState.asStateFlow()

        private val _liveStats = MutableStateFlow<RideLiveStats?>(null)
        val liveStats = _liveStats.asStateFlow()

        private val _currentSample = MutableStateFlow<com.cyclemonitor.core.model.LocationSample?>(null)
        val currentSample = _currentSample.asStateFlow()

        val lastCompletedRideId = MutableStateFlow<String?>(null)

        fun start(context: Context) = context.startForegroundService(intentFor(context, ACTION_START))
        fun pause(context: Context) = context.startService(intentFor(context, ACTION_PAUSE))
        fun resume(context: Context) = context.startService(intentFor(context, ACTION_RESUME))
        fun finish(context: Context) = context.startService(intentFor(context, ACTION_FINISH))

        private fun intentFor(context: Context, action: String) =
            Intent(context, RideRecordingService::class.java).setAction(action)
    }
}
