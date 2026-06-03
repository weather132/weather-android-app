package com.github.yun531.weatherapp.ui.forecast

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yun531.weatherapp.core.ServiceLocator
import com.github.yun531.weatherapp.data.remote.dto.AirQualityDto
import com.github.yun531.weatherapp.data.remote.dto.AlertEventDto
import com.github.yun531.weatherapp.data.remote.dto.DailyForecastDto
import com.github.yun531.weatherapp.data.remote.dto.HourlyForecastDto
import com.github.yun531.weatherapp.domain.AlertKind.Companion.noEventsLabel
import com.github.yun531.weatherapp.infra.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ForecastUiState(
    val loading: Boolean = false,
    val hourly: HourlyForecastDto? = null,
    val daily: DailyForecastDto? = null,
    val airQuality: AirQualityDto? = null,
    val warnings: List<AlertEventDto> = emptyList()
)

class ForecastViewModel : ViewModel() {

    private val api = ServiceLocator.forecastApi

    private val _stateByRegion = MutableStateFlow<Map<String, ForecastUiState>>(emptyMap())
    val stateByRegion: StateFlow<Map<String, ForecastUiState>> = _stateByRegion

    fun loadIfNeeded(regionId: String) {
        if (regionId.isBlank()) return
        if (_stateByRegion.value.containsKey(regionId)) return
        load(regionId)
    }

    fun load(regionId: String) {
        _stateByRegion.value = _stateByRegion.value + (regionId to ForecastUiState(loading = true))
        viewModelScope.launch {
            val state = ForecastUiState(
                loading = false,
                hourly = fetchHourly(regionId),
                daily = fetchDaily(regionId),
                airQuality = fetchAirQuality(regionId),
                warnings = fetchWarnings(regionId)
            )
            _stateByRegion.value = _stateByRegion.value + (regionId to state)
        }
    }

    fun refresh(regionId: String) {
        _stateByRegion.value = _stateByRegion.value - regionId
        load(regionId)
    }

    private suspend fun fetchHourly(regionId: String): HourlyForecastDto? =
        fetchTolerant("hourly", regionId) { api.getHourly(regionId).body() }

    private suspend fun fetchDaily(regionId: String): DailyForecastDto? =
        fetchTolerant("daily", regionId) { api.getDaily(regionId).body() }

    private suspend fun fetchAirQuality(regionId: String): AirQualityDto? =
        fetchTolerant("airQuality", regionId) { api.getAirQuality(regionId).body() }

    private suspend fun fetchWarnings(regionId: String): List<AlertEventDto> =
        fetchTolerant("warning", regionId) {
            ServiceLocator.alertApi.getIssuedWarnings(listOf(regionId))
        } ?: emptyList()

    private suspend fun <T> fetchTolerant(
        tag: String,
        regionId: String,
        block: suspend () -> T?
    ): T? = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (e: Exception) {
            Log.d("FORECAST", "$tag fetch failed region=$regionId msg=${e.message}")
            null
        }
    }

    fun runHourlyTriggerNowByButton(regionId: String, regionName: String) {
        if (regionId.isBlank()) return
        viewModelScope.launch {
            val ctx = ServiceLocator.appContext
            try {
                val s = ServiceLocator.settingsRepo.getOnce()
                if (!s.hourlyEnabled) return@launch

                val events = withContext(Dispatchers.IO) {
                    ServiceLocator.alertTriggerService
                        .fetchByKinds(listOf(regionId), s.enabledKinds, s.warningKinds)
                }

                if (events.isEmpty()) {
                    val kindLabel = noEventsLabel(s.enabledKinds)
                    NotificationHelper.showSimple(
                        ctx, "정각 알림 · $regionName", "$regionName: $kindLabel 없음"
                    )
                    return@launch
                }
                NotificationHelper.showAlertEvents(ctx, "정각 알림 · $regionName", events)
            } catch (e: Exception) {
                Log.d("ALERT", "manual=button hourly failed msg=${e.message}")
            }
        }
    }

    fun runDailyTriggerNowByButton(regionId: String, regionName: String) {
        if (regionId.isBlank()) return
        viewModelScope.launch {
            val ctx = ServiceLocator.appContext
            try {
                val result = withContext(Dispatchers.IO) {
                    ServiceLocator.alertTriggerService.executeDaily(listOf(regionId))
                }
                if (result.skipped) return@launch

                if (result.briefings.all { it.isEmpty() }) {
                    NotificationHelper.showSimple(
                        ctx, "일기예보 요약 · $regionName", "$regionName: 현재 요약할 소식이 없습니다"
                    )
                    return@launch
                }
                NotificationHelper.showRegionBriefings(ctx, "일기예보 요약 · $regionName", result.briefings)
            } catch (e: Exception) {
                Log.d("ALERT", "manual=button daily failed msg=${e.message}")
            }
        }
    }
}