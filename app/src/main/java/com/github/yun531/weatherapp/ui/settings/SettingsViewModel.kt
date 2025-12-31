package com.github.yun531.weatherapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yun531.weatherapp.core.ServiceLocator
import com.github.yun531.weatherapp.domain.AlertKind
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SettingsViewModel : ViewModel() {

    private val repo = ServiceLocator.settingsRepo

    val settings = repo.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        repo.getOnceOrDefault()
    )

    fun setRegion(slot: Int, regionId: String) {
        viewModelScope.launch {
            repo.setRegion(slot, regionId)
        }
    }

    fun setHourlyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repo.setHourlyEnabled(enabled)
            applyTopicSubscriptions()
        }
    }

    fun toggleKind(kind: AlertKind) {
        viewModelScope.launch {
            val cur = repo.getOnce()
            val next = cur.enabledKinds.toMutableSet().apply {
                if (contains(kind)) remove(kind) else add(kind)
            }.toSet()
            repo.setEnabledKinds(next)
        }
    }

    fun setDailyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repo.setDailyEnabled(enabled)
            applyTopicSubscriptions()
        }
    }

    fun setDailyHour(hour: Int) {
        viewModelScope.launch {
            repo.setDailyHour(hour)
            applyTopicSubscriptions()
        }
    }

    private suspend fun applyTopicSubscriptions() {
        val s = repo.getOnce()

        try {
            // hourly 토픽
            if (s.hourlyEnabled) {
                FirebaseMessaging.getInstance().subscribeToTopic("hourly").await()
                android.util.Log.d("FCM", "subscribed: hourly")
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("hourly").await()
                android.util.Log.d("FCM", "unsubscribed: hourly")
            }

            // daily 토픽: 선택한 HH 1개만 구독
            // 단순 구현(설정 화면에서만 실행): 0..23 모두 해제 후 1개 구독
            for (h in 0..23) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(dailyTopic(h)).await()
            }
            android.util.Log.d("FCM", "cleared: daily_00..daily_23")

            if (s.dailyEnabled) {
                FirebaseMessaging.getInstance().subscribeToTopic(dailyTopic(s.dailyHour)).await()
                android.util.Log.d("FCM", "subscribed: ${dailyTopic(s.dailyHour)}")
            } else {
                android.util.Log.d("FCM", "daily disabled")
            }
        } catch (e: Exception) {
            // 토픽 구독/해제 중 예외 발생 시 앱 크래시 방지 + 원인 로그
            android.util.Log.e("FCM", "topic subscribe failed", e)
        }
    }

    private fun dailyTopic(hour: Int) = "daily_%02d".format(hour.coerceIn(0, 23))
}

// settingsFlow 초기값용
private suspend fun com.github.yun531.weatherapp.data.settings.SettingsRepository.getOnceOrDefaultInternal() =
    runCatching { getOnce() }.getOrDefault(com.github.yun531.weatherapp.data.settings.AppSettings())

private fun com.github.yun531.weatherapp.data.settings.SettingsRepository.getOnceOrDefault() =
    com.github.yun531.weatherapp.data.settings.AppSettings()