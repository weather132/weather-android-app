package com.github.yun531.weatherapp.core

import android.content.Context
import com.github.yun531.weatherapp.data.remote.ApiClient
import com.github.yun531.weatherapp.data.remote.api.AlertApi
import com.github.yun531.weatherapp.data.remote.api.ForecastApi
import com.github.yun531.weatherapp.data.settings.SettingsRepository
import com.github.yun531.weatherapp.domain.AlertTriggerService

object ServiceLocator {

    lateinit var appContext: Context
        private set

    lateinit var settingsRepo: SettingsRepository
        private set

    lateinit var forecastApi: ForecastApi
        private set

    lateinit var alertApi: AlertApi
        private set

    lateinit var alertTriggerService: AlertTriggerService
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        settingsRepo = SettingsRepository(appContext)

        val retrofit = ApiClient.createRetrofit(AppConfig.BASE_URL)
        forecastApi = retrofit.create(ForecastApi::class.java)
        alertApi = retrofit.create(AlertApi::class.java)
        alertTriggerService = AlertTriggerService(settingsRepo, alertApi, forecastApi)
    }
}