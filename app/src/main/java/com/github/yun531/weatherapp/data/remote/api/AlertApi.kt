package com.github.yun531.weatherapp.data.remote.api

import com.github.yun531.weatherapp.data.remote.dto.AlertEventDto
import retrofit2.http.GET
import retrofit2.http.Query

interface AlertApi {

    @GET("notification/alerts/rain-forecast")
    suspend fun getRainForecast(@Query("regionIds") regionIds: List<String>): List<AlertEventDto>

    @GET("notification/alerts/rain-onset")
    suspend fun getRainOnset(
        @Query("regionIds") regionIds: List<String>,
        @Query("maxHour") maxHour: Int? = null
    ): List<AlertEventDto>

    @GET("notification/alerts/warning-issued")
    suspend fun getIssuedWarnings(@Query("regionIds") regionIds: List<String>): List<AlertEventDto>

    @GET("notification/alerts/summary")
    suspend fun getAlertSummary(@Query("regionIds") regionIds: List<String>): List<AlertEventDto>
}