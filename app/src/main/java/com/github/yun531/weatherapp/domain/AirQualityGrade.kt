package com.github.yun531.weatherapp.domain

enum class AirQualityGrade(val label: String) {
    GOOD("좋음"),
    MODERATE("보통"),
    BAD("나쁨"),
    VERY_BAD("매우나쁨");

    companion object {
        fun fromWire(raw: String?): AirQualityGrade? =
            raw?.let { runCatching { valueOf(it) }.getOrNull() }
    }
}