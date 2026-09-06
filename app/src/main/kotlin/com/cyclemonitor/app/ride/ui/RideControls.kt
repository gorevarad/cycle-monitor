package com.cyclemonitor.app.ride.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cyclemonitor.app.theme.CycleColors
import com.cyclemonitor.core.ride.RideState

/**
 * Ride start/pause/resume/finish controls. Deliberately the only navigation-adjacent affordance
 * shown while a ride is active -- bottom navigation is hidden during RIDING/PAUSED so a rider
 * can't accidentally leave the ride screen mid-recording (see navigation/CycleMonitorNavHost.kt).
 */
@Composable
fun RideControls(
    state: RideState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        when (state) {
            RideState.Idle, is RideState.Error -> {
                Button(onClick = onStart, colors = ButtonDefaults.buttonColors(containerColor = CycleColors.StatusGreen)) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text("START RIDE")
                }
            }
            RideState.Riding -> {
                OutlinedButton(onClick = onPause) {
                    Icon(Icons.Filled.Pause, contentDescription = null)
                    Text("PAUSE")
                }
                Button(onClick = onFinish, colors = ButtonDefaults.buttonColors(containerColor = CycleColors.StatusRed)) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Text("FINISH")
                }
            }
            RideState.Paused -> {
                Button(onClick = onResume, colors = ButtonDefaults.buttonColors(containerColor = CycleColors.StatusGreen)) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text("RESUME")
                }
                Button(onClick = onFinish, colors = ButtonDefaults.buttonColors(containerColor = CycleColors.StatusRed)) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Text("FINISH")
                }
            }
            RideState.Starting, RideState.Finishing, RideState.Completed -> Unit
        }
    }
}
