package com.babytracker.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.babytracker.data.preferences.AppPreferences
import com.babytracker.ui.dashboard.DashboardScreen
import com.babytracker.ui.i18n.LocalStrings
import com.babytracker.ui.i18n.stringsForLang
import com.babytracker.ui.main.MainScreen
import com.babytracker.ui.onboarding.LanguageSelectionScreen
import com.babytracker.ui.settings.SettingsScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

sealed class Screen(val route: String) {
    object LanguageSelection : Screen("language_selection")
    object Main : Screen("main")
    object Dashboard : Screen("dashboard")
    object Settings : Screen("settings")
}

@HiltViewModel
class AppNavigationViewModel @Inject constructor(
    val prefs: AppPreferences
) : ViewModel()

@Composable
fun AppNavigation(
    startOnboarding: Boolean,
    navController: NavHostController = rememberNavController()
) {
    val vm: AppNavigationViewModel = hiltViewModel()
    val lang by vm.prefs.language.collectAsState()
    val strings = stringsForLang(lang)

    CompositionLocalProvider(LocalStrings provides strings) {
        NavHost(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            navController = navController,
            startDestination = if (startOnboarding) Screen.LanguageSelection.route else Screen.Main.route
        ) {
            composable(Screen.LanguageSelection.route) {
                LanguageSelectionScreen(
                    onDone = {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.LanguageSelection.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Main.route) {
                MainScreen(
                    onNavigateToDashboard = { navController.navigate(Screen.Dashboard.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(onNavigateBack = { navController.popBackStack() })
            }
        }
    }
}
