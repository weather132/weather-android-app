package com.github.yun531.weatherapp.ui.forecast

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

private val FORECAST_ZONE: ZoneId = ZoneId.of("Asia/Seoul")

internal fun heroTimeLabel(announceTime: String): String? {
    val t = parseTimeToSeoul(announceTime) ?: return null
    return String.format(Locale.KOREA, "%02d/%02d %02d:%02d 업데이트", t.monthValue, t.dayOfMonth, t.hour, t.minute)
}

internal fun updateLabel(announceTime: String): String {
    val t = parseTimeToSeoul(announceTime) ?: return ""
    return String.format(Locale.KOREA, "%02d:%02d 업데이트", t.hour, t.minute)
}

private fun baseDateOf(announceTime: String): LocalDate {
    return try {
        val at = LocalDateTime.parse(announceTime)
        if (at.hour < 6) at.toLocalDate().minusDays(1) else at.toLocalDate()
    } catch (_: Exception) {
        val zdt = parseTimeToSeoul(announceTime)
        when {
            zdt == null  -> LocalDate.now(FORECAST_ZONE)
            zdt.hour < 6 -> zdt.toLocalDate().minusDays(1)
            else         -> zdt.toLocalDate()
        }
    }
}

private fun pointDate(daysAhead: Int, announceTime: String): LocalDate =
    baseDateOf(announceTime).plusDays(daysAhead.toLong())

private fun weekdayKo(date: LocalDate): String = when (date.dayOfWeek) {
    DayOfWeek.MONDAY    -> "월"
    DayOfWeek.TUESDAY   -> "화"
    DayOfWeek.WEDNESDAY -> "수"
    DayOfWeek.THURSDAY  -> "목"
    DayOfWeek.FRIDAY    -> "금"
    DayOfWeek.SATURDAY  -> "토"
    DayOfWeek.SUNDAY    -> "일"
}

internal fun dayLabel(daysAhead: Int, announceTime: String): String {
    val date = pointDate(daysAhead, announceTime)
    return if (date == LocalDate.now(FORECAST_ZONE)) "오늘" else weekdayKo(date)
}