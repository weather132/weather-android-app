package com.github.yun531.weatherapp.background.fcm

import com.github.yun531.weatherapp.background.work.TriggerWork
import com.github.yun531.weatherapp.domain.TriggerType
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class WeatherFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val type = TriggerType.fromWire(data["type"]) ?: return

        val triggerAtLocal = data["triggerAtLocal"] ?: ""
        val hour = data["hour"] ?: ""

        TriggerWork.enqueue(
            context = applicationContext,
            type = type,
            triggerAtLocal = triggerAtLocal,
            hour = hour
        )

        android.util.Log.d("FCM", "from=${message.from} data=${message.data}")
    }

    override fun onNewToken(token: String) {
        android.util.Log.d("FCM", "newToken=$token")
    }
}