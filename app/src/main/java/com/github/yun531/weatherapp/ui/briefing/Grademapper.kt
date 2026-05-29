package com.github.yun531.weatherapp.ui.briefing

import androidx.compose.ui.graphics.Color
import com.github.yun531.weatherapp.domain.AirQualityGrade
import com.github.yun531.weatherapp.domain.briefing.AirBriefing

/** 서버 직렬화 문자열("GOOD"/"MODERATE"/"BAD"/"VERY_BAD") → 기존 enum */
internal fun String.toAirQualityGrade(): AirQualityGrade? =
    runCatching { AirQualityGrade.valueOf(this) }.getOrNull()

/** ForecastScreen 과 동일한 미세먼지 색 (시각 일관성) */
internal fun gradeColor(grade: AirQualityGrade): Color = when (grade) {
    AirQualityGrade.GOOD     -> Color(0xFF4FC3F7)
    AirQualityGrade.MODERATE -> Color(0xFF66BB6A)
    AirQualityGrade.BAD      -> Color(0xFFFFA726)
    AirQualityGrade.VERY_BAD -> Color(0xFFEF5350)
}

/** AirBriefing 의 최악 등급. 둘 다 측정 없으면 null */
internal fun AirBriefing.worstGradeEnum(): AirQualityGrade? =
    representativeGrade()?.toAirQualityGrade()