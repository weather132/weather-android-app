package com.github.yun531.weatherapp

import android.app.Application
import com.github.yun531.weatherapp.infra.work.TopicSyncWorker
import com.github.yun531.weatherapp.core.ServiceLocator

class WeatherApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)

        TopicSyncWorker.enqueue(
            context = this,
            reason = "APP_START",
            forceResubscribe = false
        )
    }
}