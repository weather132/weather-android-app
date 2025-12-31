package com.github.yun531.weatherapp.data.remote.api

import com.github.yun531.weatherapp.data.remote.dto.DailyForecastDto
import com.github.yun531.weatherapp.data.remote.dto.HourlyForecastDto
import retrofit2.http.GET
import retrofit2.http.Query

interface ForecastApi {
    @GET("forecast/hourly")
    suspend fun getHourly(@Query("regionId") regionId: String): HourlyForecastDto

    @GET("forecast/daily")
    suspend fun getDaily(@Query("regionId") regionId: String): DailyForecastDto
}