package com.github.yun531.weatherapp.data.settings

import com.github.yun531.weatherapp.domain.AlertKind

data class AppSettings(
    val region1: String = "",
    val region2: String = "",
    val region3: String = "",

    val hourlyEnabled: Boolean = false,
    val enabledKinds: Set<AlertKind> = setOf(AlertKind.RAIN_ONSET, AlertKind.WARNING_ISSUED),

    val dailyEnabled: Boolean = false,
    val dailyHour: Int = 7
)