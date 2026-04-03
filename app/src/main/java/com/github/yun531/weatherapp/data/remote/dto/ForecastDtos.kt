package com.github.yun531.weatherapp.data.remote.dto

data class HourlyForecastDto(
    val regionId: String,
    val announceTime: String,
    val hourlyPoints: List<ForecastHourlyPoint>
)

data class ForecastHourlyPoint(
    val effectiveTime: String,
    val temp: Int,
    val pop: Int
)

data class DailyForecastDto(
    val regionId: String,
    val announceTime: String,
    val dailyPoints: List<ForecastDailyPoint>
)

data class ForecastDailyPoint(
    val dayAhead: Int,
    val maxTemp: Int,
    val minTemp: Int,
    val amPop: Int,
    val pmPop: Int
)