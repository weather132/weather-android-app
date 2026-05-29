package com.github.yun531.weatherapp.domain.briefing

import java.time.LocalDateTime

/**
 * 비 전망 브리핑.
 * - intervals: 24h 내 비 구간 (절대 시각)
 * - days: 7일 오전/오후 비 여부 플래그
 * - announceTime: 데이터 발표 시각. days[0] 의 기준일이며 화면의 '오늘'을 정함.
 */
data class RainBriefing(
    val intervals: List<RainInterval>,
    val days: List<DayRain>,
    val announceTime: LocalDateTime?
) {
    data class RainInterval(val start: LocalDateTime, val end: LocalDateTime)

    data class DayRain(val rainAm: Boolean, val rainPm: Boolean)

    fun hasAnyRain(): Boolean =
        intervals.isNotEmpty() || days.any { it.rainAm || it.rainPm }
}