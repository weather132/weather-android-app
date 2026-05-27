package com.github.yun531.weatherapp.domain

/**
 * 미세먼지 항목별 등급 구간 상한 (㎍/㎥).
 * - goodMax/moderateMax/badMax: 각 등급의 포함 상한
 * - displayCeil: 매우나쁨 구간 시각화 상한
 */
enum class Pollutant(
    private val goodMax: Int,
    private val moderateMax: Int,
    private val badMax: Int,
    private val displayCeil: Int
) {
    PM10(goodMax = 30, moderateMax = 80, badMax = 150, displayCeil = 300),
    PM25(goodMax = 15, moderateMax = 35, badMax = 75, displayCeil = 150);

    /** 수치를 4등분 게이지 위 위치(0f~1f)로 변환. 각 등급이 0.25폭을 차지. */
    fun fillFraction(value: Int): Float {
        val v = value.coerceAtLeast(0)
        return when {
            v <= goodMax     -> interpolate(v, 0, goodMax, base = 0.00f)
            v <= moderateMax -> interpolate(v, goodMax, moderateMax, base = 0.25f)
            v <= badMax      -> interpolate(v, moderateMax, badMax, base = 0.50f)
            else             -> interpolate(v, badMax, displayCeil, base = 0.75f)
        }
    }

    private fun interpolate(value: Int, lo: Int, hi: Int, base: Float): Float {
        val span = (hi - lo).coerceAtLeast(1)
        val within = ((value - lo).toFloat() / span).coerceIn(0f, 1f)
        return (base + within * SEGMENT_WIDTH).coerceIn(0f, 1f)
    }

    private companion object {
        const val SEGMENT_WIDTH = 0.25f
    }
}