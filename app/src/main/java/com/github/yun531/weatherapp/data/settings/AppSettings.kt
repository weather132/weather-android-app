package com.github.yun531.weatherapp.data.settings

import com.github.yun531.weatherapp.domain.AlertKind
import com.github.yun531.weatherapp.domain.WarningKind

data class AppSettings(
    val region1: String = "",
    val region2: String = "",
    val region3: String = "",

    val hourlyEnabled: Boolean = false,
    val enabledKinds: Set<AlertKind> = AlertKind.defaultSet(),
    val warningKinds: Set<WarningKind> = WarningKind.defaultSet(),

    val dailyEnabled: Boolean = false,
    val dailyHour: Int = 7
)