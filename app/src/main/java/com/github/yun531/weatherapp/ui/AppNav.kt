package com.github.yun531.weatherapp.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.yun531.weatherapp.ui.briefing.BriefingScreen
import com.github.yun531.weatherapp.ui.forecast.ForecastScreen
import com.github.yun531.weatherapp.ui.settings.SettingsScreen

private sealed class Tab(val route: String, val label: String, val icon: @Composable () -> Unit) {
    data object Briefing : Tab(
        "briefing",
        "브리핑",
        { Icon(Icons.AutoMirrored.Filled.Article, null) }
    )
    data object Forecast : Tab(
        "forecast",
        "예보",
        { Icon(Icons.AutoMirrored.Filled.List, null) })
    data object Settings : Tab(
        "settings",
        "설정",
        { Icon(Icons.Filled.Settings, null) })
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val tabs = listOf(Tab.Briefing, Tab.Forecast, Tab.Settings)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
        NavHost(navController = nav, startDestination = Tab.Briefing.route) {
            composable(Tab.Briefing.route) { BriefingScreen(padding) }
            composable(Tab.Forecast.route) { ForecastScreen(padding) }
            composable(Tab.Settings.route) { SettingsScreen(padding) }
        }
    }
}