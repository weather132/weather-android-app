package com.github.yun531.weatherapp.data.remote.api

import com.github.yun531.weatherapp.data.remote.dto.AirQualityDto
import com.github.yun531.weatherapp.data.remote.dto.DailyForecastDto
import com.github.yun531.weatherapp.data.remote.dto.HourlyForecastDto
import com.github.yun531.weatherapp.data.remote.dto.RegionAirQualityDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ForecastApi {
    @GET("forecast/hourly")
    suspend fun getHourly(@Query("regionId") regionId: String): Response<HourlyForecastDto>

    @GET("forecast/daily")
    suspend fun getDaily(@Query("regionId") regionId: String): Response<DailyForecastDto>

    @GET("forecast/air-quality")
    suspend fun getAirQuality(@Query("regionId") regionId: String): Response<AirQualityDto>

    @GET("forecast/air-quality/batch")
    suspend fun getAirQualitiesBatch(
        @Query("regionIds") regionIds: List<String>
    ): Response<List<RegionAirQualityDto>>
}