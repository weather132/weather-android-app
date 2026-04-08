package com.github.yun531.weatherapp.ui.forecast

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yun531.weatherapp.core.ServiceLocator
import com.github.yun531.weatherapp.data.remote.dto.AlertEventDto
import com.github.yun531.weatherapp.data.remote.dto.DailyForecastDto
import com.github.yun531.weatherapp.data.remote.dto.HourlyForecastDto
import com.github.yun531.weatherapp.domain.AlertKind
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
    val warnings: List<AlertEventDto> = emptyList(),
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
                val hourlyResponse = api.getHourly(regionId)
                val dailyResponse = api.getDaily(regionId)

                // 특보 조회 -- 실패해도 예보 표시에 영향 없도록 별도 처리
                val warnings = try {
                    withContext(Dispatchers.IO) {
                        ServiceLocator.alertApi.getIssuedWarnings(listOf(regionId))
                    }
                } catch (e: Exception) {
                    Log.d("WARNING", "warning fetch failed: ${e.message}")
                    emptyList()
                }

                val hourly = hourlyResponse.body()
                val daily = dailyResponse.body()

                if (hourly == null || daily == null) {
                    _stateByRegion.value = _stateByRegion.value + (regionId to ForecastUiState(
                        loading = false,
                        warnings = warnings,
                        error = "데이터 없음"
                    ))
                    return@launch
                }

                _stateByRegion.value = _stateByRegion.value + (regionId to ForecastUiState(
                    loading = false,
                    hourly = hourly,
                    daily = daily,
                    warnings = warnings
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
     * - enabledKinds 조합에 따라 alertApi 호출(getAlertSummary / getRainForecast / getIssuedWarnings)
     * - NotificationHelper.showAlertEvents 로 표시
     */
    fun runHourlyTriggerNowByButton(regionId: String, regionName: String) {
        if (regionId.isBlank()) return

        viewModelScope.launch {
            val ctx = ServiceLocator.appContext

            try {
                val s = ServiceLocator.settingsRepo.getOnce()
                if (!s.hourlyEnabled) return@launch

                val events = withContext(Dispatchers.IO) {
                    ServiceLocator.alertTriggerService
                        .fetchByKinds(listOf(regionId), s.enabledKinds)
                }

                if (events.isEmpty()) {
                    NotificationHelper.showSimple(
                        ctx,
                        regionName,
                        "활성화된 알림 종류에서 발생한 이벤트가 없습니다"
                    )
                    return@launch
                }

                NotificationHelper.showAlertEvents(ctx, "정각 알림 · $regionName", events)

            } catch (e: Exception) {
                Log.d("ALERT", "manual=button hourly failed msg=${e.message}")
            }
        }
    }
}