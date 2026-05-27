package com.github.yun531.weatherapp.domain.briefing

/**
 * 미세먼지 브리핑. PM10/PM25 각각의 등급과 수치를 보유.
 * 표시 정책:
 * - GOOD/MODERATE : 등급 라벨만
 * - BAD/VERY_BAD  : 등급 + 수치(㎍/㎥)
 * 항목별로 독립 적용한다.
 */
data class AirBriefing(
    val pm10: Int?,
    val pm10Grade: String?,
    val pm25: Int?,
    val pm25Grade: String?
) {
    fun hasAnyMeasurement(): Boolean = pm10Grade != null || pm25Grade != null

    fun pm10NeedsValue(): Boolean = isBad(pm10Grade)

    fun pm25NeedsValue(): Boolean = isBad(pm25Grade)

    /** 두 항목 중 더 나쁜 등급 (압축 행 대표값). 측정 없으면 null */
    fun representativeGrade(): String? =
        listOfNotNull(pm10Grade, pm25Grade).maxByOrNull(::severity)

    private fun isBad(grade: String?): Boolean =
        grade == Grade.BAD || grade == Grade.VERY_BAD

    private fun severity(grade: String): Int = when (grade) {
        Grade.GOOD -> 0
        Grade.MODERATE -> 1
        Grade.BAD -> 2
        Grade.VERY_BAD -> 3
        else -> -1
    }

    object Grade {
        const val GOOD = "GOOD"
        const val MODERATE = "MODERATE"
        const val BAD = "BAD"
        const val VERY_BAD = "VERY_BAD"
    }
}