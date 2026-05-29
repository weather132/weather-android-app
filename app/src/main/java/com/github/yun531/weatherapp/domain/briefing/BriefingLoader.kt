package com.github.yun531.weatherapp.domain.briefing

import com.github.yun531.weatherapp.data.remote.api.AlertApi
import com.github.yun531.weatherapp.data.remote.api.ForecastApi
import com.github.yun531.weatherapp.data.remote.dto.AlertEventDto
import com.github.yun531.weatherapp.data.remote.dto.RegionAirQualityDto
import com.github.yun531.weatherapp.domain.AlertKind
import com.github.yun531.weatherapp.domain.WarningKind
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * 일일 요약 알림과 홈 탭 브리핑의 공통 로딩 경로.
 *
 * - 비/특보/미세먼지 3개 소스를 병렬 호출
 * - 각 호출은 runCatching 으로 부분 실패에 견딤
 * - BriefingComposer 로 regionId 기준 병합
 *
 * 게이트(dailyEnabled 등)는 호출자가 처리. 이 클래스는 순수 IO+합성.
 */
class BriefingLoader(
    private val alertApi: AlertApi,
    private val forecastApi: ForecastApi
) {

    suspend fun load(
        regions: List<String>,
        kinds: Set<AlertKind>,
        warningKinds: Set<WarningKind>
    ): List<RegionBriefing> = coroutineScope {
        if (regions.isEmpty()) return@coroutineScope emptyList()

        val rain = async { if (AlertKind.RAIN_ONSET in kinds) fetchRainForecast(regions) else emptyList() }
        val warnings = async { if (AlertKind.WARNING_ISSUED in kinds) fetchWarnings(regions, warningKinds) else emptyList() }
        val air = async { if (AlertKind.AIR_POLLUTION in kinds) fetchAirQualities(regions) else emptyList() }

        BriefingComposer.compose(regions, rain.await(), warnings.await(), air.await())
    }

    private suspend fun fetchRainForecast(regions: List<String>): List<AlertEventDto> =
        runCatching { alertApi.getRainForecast(regions) }.getOrDefault(emptyList())

    private suspend fun fetchWarnings(
        regions: List<String>,
        warningKinds: Set<WarningKind>
    ): List<AlertEventDto> =
        runCatching { alertApi.getIssuedWarnings(regions, warningKinds.map { it.name }) }
            .getOrDefault(emptyList())

    private suspend fun fetchAirQualities(regions: List<String>): List<RegionAirQualityDto> =
        runCatching {
            forecastApi.getAirQualitiesBatch(regions).takeIf { it.isSuccessful }?.body() ?: emptyList()
        }.getOrDefault(emptyList())
}