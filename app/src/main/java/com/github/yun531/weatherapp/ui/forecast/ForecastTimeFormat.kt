package com.github.yun531.weatherapp.ui.forecast

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Locale

fun formatValidAtLabel(reportTime: String, validAt: String): String {
    val base = parseTimeToSeoul(reportTime)
    val t = parseTimeToSeoul(validAt) ?: return validAt

    val baseDate = (base?.toLocalDate() ?: t.toLocalDate())
    val dayDiff = ChronoUnit.DAYS.between(baseDate, t.toLocalDate())

    val hourText = String.format(Locale.KOREA, "%02d시", t.hour)
    return when (dayDiff) {
        0L -> hourText
        1L -> "내일 $hourText"
        2L -> "모레 $hourText"
        else -> "${t.monthValue}/${t.dayOfMonth} $hourText"
    }
}

fun parseTimeToSeoul(raw: String): ZonedDateTime? {
    val zone = ZoneId.of("Asia/Seoul")
    val s = raw.trim()

    // ISO-8601 OffsetDateTime
    try { return OffsetDateTime.parse(s).atZoneSameInstant(zone) } catch (_: Exception) {}

    // ISO-8601 Instant
    try { return Instant.parse(s).atZone(zone) } catch (_: Exception) {}

    // epoch sec/ms
    if (s.isNotEmpty() && s.all { it.isDigit() }) {
        val v = runCatching { s.toLong() }.getOrNull() ?: return null
        return when (s.length) {
            13 -> Instant.ofEpochMilli(v).atZone(zone)
            10 -> Instant.ofEpochSecond(v).atZone(zone)
            else -> null
        }
    }

    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyyMMddHHmmss",
        "yyyyMMddHHmm"
    )

    for (p in patterns) {
        val fmt = DateTimeFormatter.ofPattern(p)
        try {
            val ldt = LocalDateTime.parse(s, fmt)
            return ldt.atZone(zone)
        } catch (_: DateTimeParseException) {}
    }

    try { return LocalDateTime.parse(s).atZone(zone) } catch (_: Exception) {}
    return null
}