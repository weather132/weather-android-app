package com.github.yun531.weatherapp.infra.notification

sealed interface NotificationTarget {
    data object Briefing : NotificationTarget
    data class Forecast(val regionId: String?) : NotificationTarget
}