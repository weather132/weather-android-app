package com.github.yun531.weatherapp.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.github.yun531.weatherapp.ui.forecast.ForecastScreen
import com.github.yun531.weatherapp.ui.settings.SettingsScreen

private sealed class Tab(val route: String, val label: String, val icon: @Composable () -> Unit) {
    data object Forecast : Tab("forecast", "Forecast", { Icon(Icons.AutoMirrored.Filled.List, null) })
    data object Settings : Tab("settings", "Settings", { Icon(Icons.Filled.Settings, null) })
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val tabs = listOf(Tab.Forecast, Tab.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStack by nav.currentBackStackEntryAsState()
                val currentRoute = backStack?.destination?.route

                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = tab.icon,
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = Tab.Forecast.route) {
            composable(Tab.Forecast.route) { ForecastScreen(padding) }
            composable(Tab.Settings.route) { SettingsScreen(padding) }
        }
    }
}