package com.babytracker.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.babytracker.ui.dashboard.DashboardScreen
import com.babytracker.ui.main.MainScreen

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object Dashboard : Screen("dashboard")
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route)
                }
            )
        }
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
