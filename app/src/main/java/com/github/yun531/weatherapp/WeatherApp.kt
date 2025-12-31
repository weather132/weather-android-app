package com.github.yun531.weatherapp

import android.app.Application
import com.github.yun531.weatherapp.core.ServiceLocator

class WeatherApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}