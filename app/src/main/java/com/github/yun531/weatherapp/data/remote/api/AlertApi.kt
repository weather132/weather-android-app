package com.github.yun531.weatherapp.data.remote.api

import com.github.yun531.weatherapp.data.remote.dto.AlertEventDto
import retrofit2.http.GET
import retrofit2.http.Query

interface AlertApi {

    @GET("change/climate/day")
    suspend fun getClimateDay(@Query("regionIds") regionIds: List<String>): List<AlertEventDto>

    @GET("change/climate/3hour")
    suspend fun getClimate3Hour(
        @Query("regionIds") regionIds: List<String>,
        @Query("maxHour") maxHour: Int? = null
    ): List<AlertEventDto>

    @GET("change/warning")
    suspend fun getWarning(@Query("regionIds") regionIds: List<String>): List<AlertEventDto>

    @GET("change/summary")
    suspend fun getSummary(@Query("regionIds") regionIds: List<String>): List<AlertEventDto>
}