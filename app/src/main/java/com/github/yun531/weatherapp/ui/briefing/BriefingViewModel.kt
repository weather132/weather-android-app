package com.github.yun531.weatherapp.ui.briefing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.yun531.weatherapp.core.ServiceLocator
import com.github.yun531.weatherapp.domain.AlertKind
import com.github.yun531.weatherapp.domain.WarningKind
import com.github.yun531.weatherapp.domain.briefing.RegionBriefing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 홈 브리핑 화면 상태 관리.
 *
 * 설계 결정:
 * - 알림 설정(enabledKinds, dailyEnabled)과 무관하게 항상 비/특보/미세먼지 모두 로드.
 *   설정은 푸시 알림 트리거를 제어할 뿐, 홈 탭은 항상 보여준다.
 * - 지역은 SettingsRepository.selectedRegions 순서를 그대로 사용 (slot1 → 2 → 3).
 * - '오늘'은 데이터의 발표 시각(RainBriefing.announceTime) 기준. 7일 예보 화면과 일관성 확보.
 */
class BriefingViewModel : ViewModel() {

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            runCatching { loadBriefings() }
                .onSuccess { (briefings, regions) ->
                    _state.value = if (regions.isEmpty()) UiState.NoRegions
                    else UiState.Loaded(briefings, todayOf(briefings))
                }
                .onFailure { _state.value = UiState.Failed(it.message ?: "알 수 없는 오류") }
        }
    }

    private suspend fun loadBriefings(): Pair<List<RegionBriefing>, List<String>> {
        val settings = ServiceLocator.settingsRepo.getOnce()
        val regions = ServiceLocator.settingsRepo.selectedRegions(settings)
        if (regions.isEmpty()) return emptyList<RegionBriefing>() to emptyList()

        val briefings = ServiceLocator.briefingLoader.load(
            regions = regions,
            kinds = AlertKind.defaultSet(),
            warningKinds = WarningKind.defaultSet()
        )
        return briefings to regions
    }

    /** 데이터 발표 시각 기준 '오늘'. 응답에 없으면 디바이스 시각으로 fallback. */
    private fun todayOf(briefings: List<RegionBriefing>): LocalDate =
        briefings.firstOrNull { it.rain?.announceTime != null }
            ?.rain?.announceTime?.toLocalDate()
            ?: LocalDate.now()

    sealed interface UiState {
        data object Idle : UiState
        data object Loading : UiState
        data object NoRegions : UiState
        data class Loaded(val briefings: List<RegionBriefing>, val date: LocalDate) : UiState
        data class Failed(val message: String) : UiState
    }
}