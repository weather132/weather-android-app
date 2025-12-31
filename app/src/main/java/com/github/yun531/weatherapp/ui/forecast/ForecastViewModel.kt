package com.github.yun531.weatherapp.ui.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yun531.weatherapp.core.ServiceLocator
import com.github.yun531.weatherapp.data.remote.dto.DailyForecastDto
import com.github.yun531.weatherapp.data.remote.dto.HourlyForecastDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
}