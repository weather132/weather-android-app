package com.github.yun531.weatherapp.domain

enum class TriggerType {
    HOURLY_TRIGGER,
    DAILY_TRIGGER;

    companion object {
        fun fromWire(s: String?): TriggerType? = when (s) {
            "HOURLY_TRIGGER" -> HOURLY_TRIGGER
            "DAILY_TRIGGER" -> DAILY_TRIGGER
            else -> null
        }
    }
}