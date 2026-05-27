package com.github.yun531.weatherapp.domain.briefing

/**
 * 활성 기상특보 한 건.
 * - kind: RAIN/HEAT/WIND ...
 * - level: ADVISORY/WARNING
 * - eventType: NEW/UPGRADED/DOWNGRADED/EXTENDED
 */
data class WarningBriefing(
    val kind: String,
    val level: String,
    val eventType: String
)