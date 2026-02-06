package com.github.yun531.weatherapp.ui.forecast

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yun531.weatherapp.core.ServiceLocator
import com.github.yun531.weatherapp.data.remote.dto.DailyForecastDto
import com.github.yun531.weatherapp.data.remote.dto.HourlyForecastDto
import com.github.yun531.weatherapp.domain.AlertKind
import com.github.yun531.weatherapp.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ForecastUiState(
    val loading: Boolean = false,
    val hourly: HourlyForecastDto? = null,
    val daily: DailyForecastDto? = null,
    val error: String? = null
)

class ForecastViewModel : ViewModel() {

    private val api = ServiceLocator.forecastApi

    private val _stateByRegion = MutableStateFlow<Map<String, ForecastUiState>>(emptyMap())
    val stateByRegion: StateFlow<Map<String, ForecastUiState>> = _stateByRegion

    fun loadIfNeeded(regionId: String) {
        if (regionId.isBlank()) return
        val cur = _stateByRegion.value[regionId]
        if (cur?.loading == true) return
        if (cur?.hourly != null && cur.daily != null) return
        load(regionId)
    }

    fun load(regionId: String) {
        _stateByRegion.value = _stateByRegion.value + (regionId to ForecastUiState(loading = true))
        viewModelScope.launch {
            try {
                val hourly = api.getHourly(regionId)
                val daily = api.getDaily(regionId)
                _stateByRegion.value = _stateByRegion.value + (regionId to ForecastUiState(
                    loading = false,
                    hourly = hourly,
                    daily = daily
                ))
            } catch (e: Exception) {
                _stateByRegion.value = _stateByRegion.value + (regionId to ForecastUiState(
                    loading = false,
                    error = e.message ?: "error"
                ))
            }
        }
    }

    fun refresh(regionId: String) {
        _stateByRegion.value = _stateByRegion.value - regionId
        load(regionId)
    }

    /**
     * 버튼 클릭 시: TriggerFetchWorker.HOURLY_TRIGGER 와 동일한 방식으로 알림 생성
     * - settings 조회
     * - selectedRegions 조회
     * - enabledKinds 조합에 따라 alertApi 호출(getSummary / getClimate3Hour / getWarning)
     * - NotificationHelper.showAlertEvents 로 표시
     */
    fun runHourlyTriggerNowByButton(regionId: String, regionName: String) {
        if (regionId.isBlank()) return

        viewModelScope.launch {
            val ctx = ServiceLocator.appContext

            try {
                val s = ServiceLocator.settingsRepo.getOnce()
                if (!s.hourlyEnabled) return@launch

                val regions = listOf(regionId)
                val kinds = s.enabledKinds

                val events = withContext(Dispatchers.IO) {
                    when {
                        kinds.contains(AlertKind.RAIN_ONSET) && kinds.contains(AlertKind.WARNING_ISSUED) ->
                            ServiceLocator.alertApi.getSummary(regions)

                        kinds.contains(AlertKind.RAIN_ONSET) ->
                            ServiceLocator.alertApi.getClimate3Hour(regions, maxHour = null)

                        kinds.contains(AlertKind.WARNING_ISSUED) ->
                            ServiceLocator.alertApi.getWarning(regions)

                        else -> emptyList()
                    }
                }

                if (events.isEmpty()) {
                    NotificationHelper.showSimple(ctx, "테스트", "events=0 (정상: 알림 조건 미충족)")
                    return@launch
                }

                val title = "정각 알림 · $regionName"
                NotificationHelper.showAlertEvents(ctx, title, events)

            } catch (e: Exception) {
                Log.d("ALERT", "manual=button hourly failed msg=${e.message}")
            }
        }
    }
}