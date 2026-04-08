package com.github.yun531.weatherapp.infra.fcm

import android.util.Log
import com.github.yun531.weatherapp.infra.work.TopicSyncWorker
import com.github.yun531.weatherapp.infra.work.TriggerWork
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

        Log.d("FCM", "from=${message.from} data=${message.data}")
    }

    override fun onNewToken(token: String) {
        Log.d("FCM", "newToken=$token")

        // 토큰 변경 시점엔 토픽 매핑이 꼬일 수 있으니 “강제 동기화”
        TopicSyncWorker.enqueue(
            context = applicationContext,
            reason = "NEW_TOKEN",
            forceResubscribe = true,
            tokenHint = token
        )
    }
}