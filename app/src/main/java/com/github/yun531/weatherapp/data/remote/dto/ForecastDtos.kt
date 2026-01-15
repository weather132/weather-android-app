package com.github.yun531.weatherapp.data.remote.dto

data class HourlyForecastDto(
    val regionId: String,
    val reportTime: String,
    val hours: List<HourlyPointDto>
)

data class HourlyPointDto(
    val validAt: String,
    val temp: Int,
    val pop: Int
)

data class DailyForecastDto(
    val regionId: String,
    val reportTime: String,
    val days: List<DailyPointDto>
)

data class DailyPointDto(
    val dayOffset: Int,
    val maxTemp: Int,
    val minTemp: Int,
    val amPop: Int,
    val pmPop: Int
)