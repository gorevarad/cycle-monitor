package com.cyclemonitor.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyclemonitor.app.navigation.CycleMonitorNavHost
import com.cyclemonitor.app.theme.AppThemeMode
import com.cyclemonitor.app.theme.CycleMonitorTheme
import com.cyclemonitor.app.data.settings.ThemePreference

class MainActivity : ComponentActivity() {

    private val container get() = (application as CycleMonitorApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = com.cyclemonitor.app.data.settings.UserSettings(),
            )
            val themeMode = when (settings.theme) {
                ThemePreference.DARK -> AppThemeMode.DARK
                ThemePreference.LIGHT -> AppThemeMode.LIGHT
                ThemePreference.SYSTEM -> AppThemeMode.SYSTEM
            }
            CycleMonitorTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CycleMonitorNavHost(container = container)
                }
            }
        }
    }
}
