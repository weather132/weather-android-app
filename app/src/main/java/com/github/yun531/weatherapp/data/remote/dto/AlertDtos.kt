package com.github.yun531.weatherapp.data.remote.dto

import com.google.gson.JsonObject

data class AlertEventDto(
    val type: String,
    val regionId: String,
    val occurredAt: String,
    val payload: JsonObject
)
