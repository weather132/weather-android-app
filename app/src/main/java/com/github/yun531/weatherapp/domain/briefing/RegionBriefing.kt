package com.github.yun531.weatherapp.domain.briefing

/**
 * 한 지역의 종합 브리핑.
 * - rain: 비 소식
 * - warnings: 활성 특보
 * - air: 미세먼지 정보
 */
data class RegionBriefing(
    val regionId: String,
    val rain: RainBriefing?,
    val warnings: List<WarningBriefing>,
    val air: AirBriefing?
) {
    fun isEmpty(): Boolean =
        (rain == null || !rain.hasAnyRain()) &&
                warnings.isEmpty() &&
                (air == null || !air.hasAnyMeasurement())
}