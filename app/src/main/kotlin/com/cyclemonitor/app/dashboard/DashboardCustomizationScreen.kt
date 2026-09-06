package com.cyclemonitor.app.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cyclemonitor.app.data.repository.CUSTOM_PROFILE_ACCENT_CHOICES
import com.cyclemonitor.app.di.AppContainer
import com.cyclemonitor.app.di.ViewModelFactory
import com.cyclemonitor.app.theme.CycleColors
import com.cyclemonitor.core.model.DashboardMetric
import com.cyclemonitor.core.model.DashboardProfile

@Composable
fun DashboardCustomizationScreen(container: AppContainer) {
    val viewModel: DashboardCustomizationViewModel = viewModel(
        factory = ViewModelFactory { DashboardCustomizationViewModel(container.dashboardProfileRepository, container.settingsRepository) },
    )
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedProfileId.collectAsStateWithLifecycle()
    val selected = profiles.find { it.id == selectedId } ?: profiles.firstOrNull()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("DASHBOARD CUSTOMIZATION", style = MaterialTheme.typography.headlineSmall, color = CycleColors.TextPrimary)
        Text(
            "Choose what shows on the ride dashboard and in what order.",
            style = MaterialTheme.typography.bodySmall,
            color = CycleColors.TextSecondary,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
            profiles.forEach { profile ->
                FilterChip(
                    selected = profile.id == selectedId,
                    onClick = { viewModel.selectProfile(profile.id) },
                    label = { Text(profile.name) },
                )
            }
        }

        if (selected == null) return@Column

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 16.dp)) {
            IconButton(onClick = { viewModel.duplicateProfile(selected) }) { Icon(Icons.Filled.Add, contentDescription = "Duplicate as new profile") }
            IconButton(onClick = { viewModel.deleteProfile(selected.id) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete profile") }
        }

        Text("VISIBLE METRICS (in order)", style = MaterialTheme.typography.labelLarge, color = CycleColors.NavigationCyan)
        selected.metrics.forEachIndexed { index, metric ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(metric.name.replace('_', ' '), color = CycleColors.TextPrimary)
                Row {
                    IconButton(onClick = { viewModel.reorderMetric(selected, metric, moveUp = true) }, enabled = index > 0) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move up")
                    }
                    IconButton(onClick = { viewModel.reorderMetric(selected, metric, moveUp = false) }, enabled = index < selected.metrics.lastIndex) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move down")
                    }
                    IconButton(onClick = { viewModel.toggleMetric(selected, metric) }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Hide")
                    }
                }
            }
        }

        val hiddenMetrics = DashboardMetric.entries.filter { it !in selected.metrics }
        if (hiddenMetrics.isNotEmpty()) {
            Text("AVAILABLE METRICS", style = MaterialTheme.typography.labelLarge, color = CycleColors.NavigationCyan, modifier = Modifier.padding(top = 16.dp))
            hiddenMetrics.forEach { metric ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { viewModel.toggleMetric(selected, metric) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(metric.name.replace('_', ' '), color = CycleColors.TextSecondary)
                    Icon(Icons.Filled.Add, contentDescription = "Show", tint = CycleColors.TextSecondary)
                }
            }
        }

        Text("SPEED GAUGE MAX (km/h)", style = MaterialTheme.typography.labelLarge, color = CycleColors.NavigationCyan, modifier = Modifier.padding(top = 16.dp))
        Slider(
            value = selected.speedGaugeMaxKmh.toFloat(),
            valueRange = 30f..120f,
            onValueChange = { viewModel.setSpeedGaugeMax(selected, it.toInt()) },
        )
        Text("${selected.speedGaugeMaxKmh} km/h", color = CycleColors.TextSecondary, style = MaterialTheme.typography.bodySmall)

        Text("POWER GAUGE MAX (W)", style = MaterialTheme.typography.labelLarge, color = CycleColors.NavigationCyan, modifier = Modifier.padding(top = 16.dp))
        Slider(
            value = selected.powerGaugeMaxWatts.toFloat(),
            valueRange = 200f..1000f,
            onValueChange = { viewModel.setPowerGaugeMax(selected, it.toInt()) },
        )
        Text("${selected.powerGaugeMaxWatts} W", color = CycleColors.TextSecondary, style = MaterialTheme.typography.bodySmall)

        Text("ACCENT COLOR", style = MaterialTheme.typography.labelLarge, color = CycleColors.NavigationCyan, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CUSTOM_PROFILE_ACCENT_CHOICES.forEach { argb ->
                val color = Color(argb)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color, CircleShape)
                        .clickable { viewModel.setAccentColor(selected, argb) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (argb == selected.accentColorArgb) {
                        Box(modifier = Modifier.size(12.dp).background(CycleColors.TextPrimary, CircleShape))
                    }
                }
            }
        }
    }
}
