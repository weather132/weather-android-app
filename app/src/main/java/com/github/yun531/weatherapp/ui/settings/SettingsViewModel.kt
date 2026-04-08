package com.github.yun531.weatherapp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yun531.weatherapp.infra.work.TopicSyncWorker
import com.github.yun531.weatherapp.core.ServiceLocator
import com.github.yun531.weatherapp.domain.AlertKind
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val repo = ServiceLocator.settingsRepo

    val settings = repo.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        com.github.yun531.weatherapp.data.settings.AppSettings()
    )

    fun setRegion(slot: Int, regionId: String) {
        viewModelScope.launch {
            repo.setRegion(slot, regionId)
            // 지역 변경은 토픽과 무관(알림 데이터만 바뀜) → sync 불필요
        }
    }

    fun setHourlyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repo.setHourlyEnabled(enabled)
            TopicSyncWorker.enqueue(
                context = ServiceLocator.appContext,
                reason = "SETTINGS_CHANGED",
                forceResubscribe = false
            )
        }
    }

    fun toggleKind(kind: AlertKind) {
        viewModelScope.launch {
            val cur = repo.getOnce()
            val next = cur.enabledKinds.toMutableSet().apply {
                if (contains(kind)) remove(kind) else add(kind)
            }.toSet()
            repo.setEnabledKinds(next)
            // 알림 종류는 “API 호출/알림 생성 로직”에만 영향 → 토픽 sync 불필요
        }
    }

    fun setDailyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repo.setDailyEnabled(enabled)
            TopicSyncWorker.enqueue(
                context = ServiceLocator.appContext,
                reason = "SETTINGS_CHANGED",
                forceResubscribe = false
            )
        }
    }

    fun setDailyHour(hour: Int) {
        viewModelScope.launch {
            repo.setDailyHour(hour)
            TopicSyncWorker.enqueue(
                context = ServiceLocator.appContext,
                reason = "SETTINGS_CHANGED",
                forceResubscribe = false
            )
        }
    }
}