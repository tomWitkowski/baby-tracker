package com.babytracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
            BabyTrackerTheme {
                AppNavigation(startOnboarding = appPreferences.isFirstLaunch)
            }
        }
    }
}
