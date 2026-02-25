package com.babytracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.babytracker.data.preferences.AppPreferences
import com.babytracker.ui.navigation.AppNavigation
import com.babytracker.ui.theme.BabyTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by appPreferences.themeMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val useDark = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> systemDark
            }
            BabyTrackerTheme(darkTheme = useDark) {
                AppNavigation(startOnboarding = appPreferences.isFirstLaunch)
            }
        }
    }
}
