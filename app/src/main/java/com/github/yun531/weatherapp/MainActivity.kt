package com.github.yun531.weatherapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import com.github.yun531.weatherapp.ui.AppNav
import com.github.yun531.weatherapp.ui.NavRoutes
import com.github.yun531.weatherapp.ui.theme.WeatherAppTheme

class MainActivity : ComponentActivity() {

    private val requestNotiPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val startRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        startRoute.value = routeFromIntent(intent)

        setContent {
            WeatherAppTheme {
                AppNav(
                    startRoute = startRoute.value,
                    onStartRouteHandled = { startRoute.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        startRoute.value = routeFromIntent(intent)
    }

    private fun routeFromIntent(intent: Intent?): String? =
        intent?.getStringExtra(NavRoutes.EXTRA_START_ROUTE)

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 33) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) return
        requestNotiPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}