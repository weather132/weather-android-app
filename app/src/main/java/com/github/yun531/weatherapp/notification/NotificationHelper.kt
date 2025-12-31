package com.github.yun531.weatherapp.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.github.yun531.weatherapp.R
import com.github.yun531.weatherapp.data.region.RegionCatalog
import com.github.yun531.weatherapp.data.remote.dto.AlertEventDto
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.random.Random

object NotificationHelper {

    private const val CHANNEL_ID = "weather_alerts"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Weather 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(channel)
        }
    }

    /**
     * Android 13(API 33)+ 에서는 POST_NOTIFICATIONS 런타임 권한이 거부될 수 있음.
     * - 권한이 없으면 notify 호출을 하지 않고 조용히 종료.
     */
    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 디버그용: events가 비어도 파이프라인(FCM 수신 → Worker 실행 → 알림 생성) 확인을 위해
     * 알림 1개를 강제로 띄움.
     */
    fun showSimple(context: Context, title: String, text: String) {
        ensureChannel(context)

        if (!canPostNotifications(context)) return

        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return

        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()

        nm.notify(999001, n)
    }

    fun showAlertEvents(context: Context, title: String, events: List<AlertEventDto>) {
        if (events.isEmpty()) return
        ensureChannel(context)

        if (!canPostNotifications(context)) return

        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return

        val catalog = RegionCatalog.get(context)

        val lines = mutableListOf<String>()
        events.forEach { e ->
            val regionName = catalog.nameOf(e.regionId)

            when (e.type) {
                "RAIN_FORECAST" -> {
                    // 여러 줄로 풀어서 누적 (줄바꿈 효과)
                    val subLines = formatRainForecastLines(e.payload, e.occurredAt)
                    subLines.forEach { l ->
                        lines += "$regionName: $l"
                    }
                }
                "RAIN_ONSET" -> {
                    lines += "$regionName: ${formatRainOnset(e.payload)}"
                }
                "WARNING_ISSUED" -> {
                    lines += "$regionName: ${formatWarning(e.payload)}"
                }
                else -> {
                    lines += "$regionName: ${e.type}"
                }
            }
        }

        if (lines.isEmpty()) return

        val style = NotificationCompat.InboxStyle()
        lines.take(7).forEach { style.addLine(it) }
        if (lines.size > 7) style.setSummaryText("+${lines.size - 7} more")

        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            // 접힌 상태에서 대표 1줄
            .setContentText(lines.first())
            // 펼치면 여러 줄 표시
            .setStyle(style)
            .setAutoCancel(true)
            .build()

        nm.notify(Random.nextInt(Int.MAX_VALUE), n)
    }

    /**
     * RAIN_FORECAST를 "여러 줄"로 가공:
     * - hourlyParts: 각 시간 구간을 1줄씩 ("비 예보: 오늘 23시~내일 2시")
     * - dayParts: 요약 1줄 ("7일: 모레 오후, D+5 오전" ...)
     *
     * occurredAt 기준:
     * - "2025-12-31T20:49:00" -> 정각으로 내림(baseHour=20:00)
     * - hourlyParts [3,6] -> baseHour+3h ~ +6h => 오늘 23시~내일 2시
     */
    private fun formatRainForecastLines(payload: JsonObject, occurredAtRaw: String?): List<String> {
        val zone = ZoneId.systemDefault()

        val occurredAt = occurredAtRaw?.let { parseOccurredAt(it, zone) }
        val baseHour = occurredAt?.truncatedTo(ChronoUnit.HOURS)

        val hourlyArr = payload.getAsJsonArray("hourlyParts")
        val dayArr = payload.getAsJsonArray("dayParts")

        val out = mutableListOf<String>()

        // 1) 시간대(가까운 예보) 라인들
        val hourlySegments: List<String> = when {
            hourlyArr == null || hourlyArr.size() == 0 -> emptyList()
            baseHour != null -> formatHourRangesPretty(hourlyArr, baseHour)
            else -> formatHourRangesFallback(hourlyArr) // occurredAt이 없을 때 폴백
        }

        if (hourlySegments.isEmpty()) {
            out += "가까운 시간대 강수 없음"
        } else {
            hourlySegments.forEach { seg ->
                out += "비 예보: $seg"
            }
        }

        // 2) 7일(오전/오후) 요약 라인
        val baseDate = (baseHour?.toLocalDate() ?: LocalDate.now(zone))
        val daySegments: List<String> = when {
            dayArr == null || dayArr.size() == 0 -> emptyList()
            else -> formatDayPartsPretty(dayArr, baseDate)
        }
        if (daySegments.isNotEmpty()) {
            // 너무 길어지면 2개까지만 보여주고 나머지는 축약
            val dayText = if (daySegments.size <= 2) {
                daySegments.joinToString(", ")
            } else {
                daySegments.take(2).joinToString(", ") + " 외 ${daySegments.size - 2}일"
            }
            out += "7일: $dayText"
        }

        return out
    }

    private fun parseOccurredAt(raw: String, zone: ZoneId): ZonedDateTime {
        return try {
            ZonedDateTime.parse(raw).withZoneSameInstant(zone)
        } catch (_: Exception) {
            try {
                OffsetDateTime.parse(raw).atZoneSameInstant(zone)
            } catch (_: Exception) {
                LocalDateTime.parse(raw).atZone(zone)
            }
        }
    }

    /**
     * hourlyParts: [[3,6],[16,18], ...]
     * baseHour(예: 2025-12-31T20:00) + offset(hours) => "오늘 23시~내일 2시"
     */
    private fun formatHourRangesPretty(arr: JsonArray, baseHour: ZonedDateTime): List<String> {
        val out = mutableListOf<String>()

        arr.forEach { el ->
            if (!el.isJsonArray) return@forEach
            val a = el.asJsonArray
            if (a.size() < 2) return@forEach

            val s = a[0].asInt
            val e = a[1].asInt

            val start = baseHour.plusHours(s.toLong())
            val end = baseHour.plusHours(e.toLong())

            out += formatZonedRange(start, end, baseHour.toLocalDate())
        }

        return out
    }

    private fun formatZonedRange(start: ZonedDateTime, end: ZonedDateTime, baseDate: LocalDate): String {
        val startLabel = dayLabel(start.toLocalDate(), baseDate)
        val endLabel = dayLabel(end.toLocalDate(), baseDate)

        val sh = start.hour
        val eh = end.hour

        return if (start.toInstant() == end.toInstant()) {
            "$startLabel ${sh}시"
        } else {
            if (start.toLocalDate() == end.toLocalDate()) {
                "$startLabel ${sh}~${eh}시"
            } else {
                "$startLabel ${sh}시~$endLabel ${eh}시"
            }
        }
    }

    private fun dayLabel(date: LocalDate, base: LocalDate): String {
        val diff = ChronoUnit.DAYS.between(base, date).toInt()
        return when (diff) {
            0 -> "오늘"
            1 -> "내일"
            2 -> "모레"
            else -> "D+$diff"
        }
    }

    /**
     * dayParts: 7일치 [오전,오후] 플래그 (0/1)
     * idx = baseDate로부터 n일 후
     * 예: idx=2, [0,1] => "모레 오후"
     */
    private fun formatDayPartsPretty(arr: JsonArray, baseDate: LocalDate): List<String> {
        val out = mutableListOf<String>()

        arr.forEachIndexed { idx, el ->
            if (!el.isJsonArray) return@forEachIndexed
            val a = el.asJsonArray

            val am = a.getOrNull(0)?.asInt ?: 0
            val pm = a.getOrNull(1)?.asInt ?: 0
            if (am == 0 && pm == 0) return@forEachIndexed

            val date = baseDate.plusDays(idx.toLong())
            val dLabel = dayLabel(date, baseDate)

            val ap = when {
                am == 1 && pm == 1 -> "오전/오후"
                am == 1 -> "오전"
                pm == 1 -> "오후"
                else -> ""
            }

            out += "$dLabel $ap".trim()
        }

        return out
    }

    /**
     * occurredAt이 없을 때는 offset을 그대로 보여주는 폴백
     */
    private fun formatHourRangesFallback(arr: JsonArray): List<String> {
        val out = mutableListOf<String>()
        arr.forEach { el ->
            if (!el.isJsonArray) return@forEach
            val a = el.asJsonArray
            if (a.size() < 2) return@forEach
            val s = a[0].asInt
            val e = a[1].asInt
            out += if (s == e) "${s}시간 후" else "${s}~${e}시간 후"
        }
        return out
    }

    private fun formatRainOnset(payload: JsonObject): String {
        val pop = payload.get("pop")?.asInt
        val hourOffset = payload.get("hourOffset")?.asInt
        return if (pop != null && hourOffset != null) {
            "비 예보: ${hourOffset}시간 후 (강수확률 ${pop}%)"
        } else "비 예보"
    }

    private fun formatWarning(payload: JsonObject): String {
        val level = payload.get("level")?.asString ?: "?"
        val kind = payload.get("kind")?.asString ?: "?"
        return "기상특보: $kind ($level)"
    }

    private fun JsonArray.getOrNull(i: Int) = if (i in 0 until size()) get(i) else null
}