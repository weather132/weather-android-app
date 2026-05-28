package com.github.yun531.weatherapp.infra.notification

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
import com.github.yun531.weatherapp.domain.AirQualityGrade
import com.github.yun531.weatherapp.domain.WarningLabels
import com.github.yun531.weatherapp.domain.briefing.AirBriefing
import com.github.yun531.weatherapp.domain.briefing.RainBriefing
import com.github.yun531.weatherapp.domain.briefing.RegionBriefing
import com.github.yun531.weatherapp.domain.briefing.WarningBriefing
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
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

        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return

        // Lint(MissingPermission) 대응: notify 직전에 명시적으로 체크
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

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

        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return

        // Lint(MissingPermission) 대응: notify 직전에 명시적으로 체크
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val catalog = RegionCatalog.get(context)

        val lines = mutableListOf<String>()
        events.forEach { e ->
            val regionName = catalog.nameOf(e.regionId)

            when (e.type) {
                "RAIN_FORECAST" -> {
                    val subLines = formatRainForecastLines(e.payload, e.occurredAt)
                    subLines.forEach { l ->
                        lines += "$regionName: $l"
                    }
                }
                "RAIN_ONSET" -> {
                    lines += "$regionName: ${formatRainOnset(e.payload, e.occurredAt)}"
                }
                "WARNING_ISSUED" -> {
                    lines += "$regionName: ${formatWarning(e.payload)}"
                }
                "AIR_POLLUTION" -> {
                    lines += "$regionName: ${formatAirPollution(e.payload)}"
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
            .setContentText(lines.first())
            .setStyle(style)
            .setAutoCancel(true)
            .build()

        nm.notify(Random.nextInt(Int.MAX_VALUE), n)
    }

    /**
     * 일일 요약/브리핑 표시 로직
     */
    fun showRegionBriefings(context: Context, title: String, briefings: List<RegionBriefing>) {
        if (briefings.isEmpty()) return
        ensureChannel(context)

        val nm = NotificationManagerCompat.from(context)
        if (!nm.areNotificationsEnabled()) return

        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val catalog = RegionCatalog.get(context)
        val lines = buildBriefingLines(briefings, catalog)
        if (lines.isEmpty()) return

        val style = NotificationCompat.InboxStyle()
        lines.take(7).forEach { style.addLine(it) }
        if (lines.size > 7) style.setSummaryText("+${lines.size - 7} more")

        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(lines.first())
            .setStyle(style)
            .setAutoCancel(true)
            .build()

        nm.notify(Random.nextInt(Int.MAX_VALUE), n)
    }

    private fun buildBriefingLines(
        briefings: List<RegionBriefing>,
        catalog: RegionCatalog
    ): List<String> {
        val lines = mutableListOf<String>()
        briefings.forEach { b ->
            val regionName = catalog.nameOf(b.regionId)
            briefingSegments(b).forEach { seg -> lines += "$regionName: $seg" }
        }
        return lines
    }

    /** 한 지역의 비/특보/미세먼지를 표시 줄(segment)들로 변환 */
    private fun briefingSegments(b: RegionBriefing): List<String> {
        val segments = mutableListOf<String>()
        b.rain?.let { segments += rainSegments(it) }
        b.warnings.forEach { segments += warningSegment(it) }
        b.air?.let { air -> airSegments(air).forEach { segments += it } }
        return segments.ifEmpty { listOf("특이사항 없음") }
    }

    private fun rainSegments(rain: RainBriefing): List<String> {
        if (!rain.hasAnyRain()) return emptyList()

        val out = mutableListOf<String>()
        val fmt = DateTimeFormatter.ofPattern("M/d HH시")
        rain.intervals.forEach { iv ->
            out += "비 ${iv.start.format(fmt)}~${iv.end.format(fmt)}"
        }
        val rainyDays = rain.days.count { it.rainAm || it.rainPm }
        if (rainyDays > 0) out += "7일 중 ${rainyDays}일 비"
        return out
    }

    private fun warningSegment(w: WarningBriefing): String =
        "특보 ${kindLabel(w.kind)} ${levelLabel(w.level)}"

    private fun airSegments(air: AirBriefing): List<String> {
        if (!air.hasAnyMeasurement()) return emptyList()

        val out = mutableListOf<String>()
        air.pm10Grade?.let { out += pmSegment("미세먼지", it, air.pm10, air.pm10NeedsValue()) }
        air.pm25Grade?.let { out += pmSegment("초미세먼지", it, air.pm25, air.pm25NeedsValue()) }
        return out
    }

    private fun pmSegment(label: String, grade: String, value: Int?, withValue: Boolean): String =
        if (withValue && value != null) "$label ${gradeLabel(grade)} ${value}㎍/㎥"
        else "$label ${gradeLabel(grade)}"

    private fun gradeLabel(grade: String): String = when (grade) {
        AirBriefing.Grade.GOOD -> "좋음"
        AirBriefing.Grade.MODERATE -> "보통"
        AirBriefing.Grade.BAD -> "나쁨"
        AirBriefing.Grade.VERY_BAD -> "매우나쁨"
        else -> grade
    }

    private fun levelLabel(level: String): String = when (level) {
        "WARNING" -> "경보"
        "ADVISORY" -> "주의보"
        else -> level
    }

    private fun kindLabel(kind: String): String = when (kind) {
        "RAIN" -> "호우"
        "HEAT" -> "폭염"
        "WIND" -> "강풍"
        "COLD" -> "한파"
        "SNOW" -> "대설"
        "DRY" -> "건조"
        else -> kind
    }

    /**
     * RAIN_FORECAST를 "여러 줄"로 가공:
     * - hourlyParts: 각 시간 구간을 1줄씩 ("비 예보: 오늘 23시~내일 2시")
     * - dayParts: 요약 1줄 ("7일: 모레 오후, D+5 오전" ...)
     *
     * occurredAt 기준:
     * - "2025-12-31T20:49:00" -> 정각으로 내림(baseHour=20:00)
     * - hourlyParts [3,6] -> baseHour+3h ~ +6h => 오늘 23시~내일 2시
     * - hourlyParts {start,end} -> start/end 절대시각으로 처리
     */
    private fun formatRainForecastLines(payload: JsonObject, occurredAtRaw: String?): List<String> {
        val zone = ZoneId.systemDefault()
        val occurredAt = occurredAtRaw?.let { parseOccurredAt(it, zone) }
        val baseDate = occurredAt?.toLocalDate() ?: LocalDate.now(zone)
        val baseHour = (occurredAt ?: ZonedDateTime.now(zone)).truncatedTo(ChronoUnit.HOURS)

        val hourlyArr = payload.getAsJsonArray("hourlyParts")
        val dayArr = payload.getAsJsonArray("dayParts")
        val out = mutableListOf<String>()

        val hourlySegments: List<String> = when {
            hourlyArr == null || hourlyArr.size() == 0 -> emptyList()
            else -> formatHourRangesSmart(hourlyArr, baseDate, zone, baseHour)
        }

        if (hourlySegments.isEmpty()) out += "가까운 시간대 강수 없음"
        else hourlySegments.forEach { out += "비 예보: $it" }

        val daySegments: List<String> = when {
            dayArr == null || dayArr.size() == 0 -> emptyList()
            else -> formatDayPartsPretty(dayArr, baseDate)
        }
        if (daySegments.isNotEmpty()) {
            val dayText = if (daySegments.size <= 2) {
                daySegments.joinToString(", ")
            } else {
                daySegments.take(2).joinToString(", ") + " 외 ${daySegments.size - 2}일"
            }
            out += "──────── 7일 ────────"
            out += "[7일 요약] >> $dayText"
        }
        return out
    }

    private fun formatHourRangesSmart(
        arr: JsonArray,
        baseDate: LocalDate,
        zone: ZoneId,
        baseHour: ZonedDateTime
    ): List<String> {
        val out = mutableListOf<String>()
        arr.forEach { el ->
            when {
                el.isJsonObject -> {
                    val o = el.asJsonObject
                    val sRaw = o.get("start")?.asString ?: return@forEach
                    val eRaw = o.get("end")?.asString ?: sRaw
                    val start = parseIsoToZoned(sRaw, zone) ?: return@forEach
                    val end = parseIsoToZoned(eRaw, zone) ?: return@forEach
                    out += formatZonedRange(start, end, baseDate)
                }
                el.isJsonArray -> {
                    val a = el.asJsonArray
                    if (a.size() < 2) return@forEach
                    val p0 = a[0]
                    val p1 = a[1]
                    val bothNumber = p0.isJsonPrimitive && p1.isJsonPrimitive &&
                            p0.asJsonPrimitive.isNumber && p1.asJsonPrimitive.isNumber

                    if (bothNumber) {
                        val s = p0.asInt
                        val e = p1.asInt
                        val start = baseHour.plusHours(s.toLong())
                        val end = baseHour.plusHours(e.toLong())
                        out += formatZonedRange(start, end, baseDate)
                        return@forEach
                    }

                    val sRaw = runCatching { p0.asString }.getOrNull() ?: return@forEach
                    val eRaw = runCatching { p1.asString }.getOrNull() ?: return@forEach
                    val start = parseIsoToZoned(sRaw, zone) ?: return@forEach
                    val end = parseIsoToZoned(eRaw, zone) ?: return@forEach
                    out += formatZonedRange(start, end, baseDate)
                }
                else -> Unit
            }
        }
        return out
    }

    private fun parseIsoToZoned(raw: String, zone: ZoneId): ZonedDateTime? {
        val s = raw.trim()
        try { return OffsetDateTime.parse(s).atZoneSameInstant(zone) } catch (_: Exception) {}
        try { return Instant.parse(s).atZone(zone) } catch (_: Exception) {}
        return try {
            LocalDateTime.parse(s).atZone(zone)
        } catch (_: Exception) {
            runCatching {
                val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
                LocalDateTime.parse(s, fmt).atZone(zone)
            }.getOrNull()
        }
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

    private fun formatZonedRange(start: ZonedDateTime, end: ZonedDateTime, baseDate: LocalDate): String {
        val (s, e) = if (start.toInstant() <= end.toInstant()) start to end else end to start
        val startLabel = dayLabel(s.toLocalDate(), baseDate)
        val endLabel = dayLabel(e.toLocalDate(), baseDate)
        val sh = s.hour
        val eh = e.hour

        return if (s.toInstant() == e.toInstant()) {
            "$startLabel ${sh}시"
        } else {
            if (s.toLocalDate() == e.toLocalDate()) {
                "$startLabel ${sh}시~${eh}시"
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

    private fun formatDayPartsPretty(arr: JsonArray, baseDate: LocalDate): List<String> {
        val out = mutableListOf<String>()
        arr.forEachIndexed { idx, el ->
            val (am, pm) = when {
                el.isJsonObject -> {
                    val o = el.asJsonObject
                    val amB = o.get("rainAm")?.asBoolean ?: false
                    val pmB = o.get("rainPm")?.asBoolean ?: false
                    amB to pmB
                }
                el.isJsonArray -> {
                    val a = el.asJsonArray
                    val amI = a.getOrNull(0)?.asInt ?: 0
                    val pmI = a.getOrNull(1)?.asInt ?: 0
                    (amI == 1) to (pmI == 1)
                }
                else -> false to false
            }

            if (!am && !pm) return@forEachIndexed

            val date = baseDate.plusDays(idx.toLong())
            val dLabel = dayLabel(date, baseDate)
            val ap = when {
                am && pm -> "오전/오후"
                am -> "오전"
                pm -> "오후"
                else -> ""
            }
            out += "$dLabel $ap".trim()
        }
        return out
    }

    private fun formatHourRangesFallback(arr: JsonArray): List<String> {
        val out = mutableListOf<String>()
        arr.forEach { el ->
            if (!el.isJsonArray) return@forEach
            val a = el.asJsonArray
            if (a.size() < 2) return@forEach
            val p0 = a[0]
            val p1 = a[1]
            val bothNumber = p0.isJsonPrimitive && p1.isJsonPrimitive &&
                    p0.asJsonPrimitive.isNumber && p1.asJsonPrimitive.isNumber
            if (!bothNumber) return@forEach
            val s = p0.asInt
            val e = p1.asInt
            out += if (s == e) "${s}시간 후" else "${s}~${e}시간 후"
        }
        return out
    }

    private fun formatRainOnset(payload: JsonObject, occurredAtRaw: String?): String {
        val zone = ZoneId.systemDefault()
        val pop = payload.get("pop")?.asInt
        val validAtRaw = payload.get("effectiveTime")?.asString

        if (pop == null || validAtRaw.isNullOrBlank()) return "비 예보"

        val occurredAt = occurredAtRaw?.let { runCatching { parseOccurredAt(it, zone) }.getOrNull() }
        val baseDate = occurredAt?.toLocalDate() ?: LocalDate.now(zone)
        val t = parseIsoToZoned(validAtRaw, zone)
        val label = if (t != null) formatZonedRange(t, t, baseDate) else validAtRaw

        return "비 예보: $label (강수확률 ${pop}%)"
    }

    private fun formatWarning(payload: JsonObject): String {
        val kind = payload.get("kind")?.asString ?: "?"
        val level = payload.get("level")?.asString ?: "?"
        val eventType = payload.get("eventType")?.asString
        val prevLevel = payload.get("prevLevel")?.asString

        val kindLabel = WarningLabels.kindLabel(kind)
        val levelLabel = WarningLabels.levelLabel(level)

        return when (eventType) {
            "NEW"        -> "$kindLabel $levelLabel 발령"
            "UPGRADED"   -> "$kindLabel ${WarningLabels.levelLabel(prevLevel)} -> $levelLabel 격상"
            "DOWNGRADED" -> "$kindLabel ${WarningLabels.levelLabel(prevLevel)} -> $levelLabel 하향"
            "EXTENDED"   -> "$kindLabel $levelLabel 연장"
            else         -> "기상특보: $kindLabel ($levelLabel)"
        }
    }

    private fun formatAirPollution(payload: JsonObject): String {
        val type = payload.get("pollutionType")?.takeUnless { it.isJsonNull }?.asString
        val grade = AirQualityGrade.fromWire(
            payload.get("grade")?.takeUnless { it.isJsonNull }?.asString
        )?.label ?: ""
        val value = payload.get("value")?.takeUnless { it.isJsonNull }?.asInt
        val name = when (type) {
            "PM10" -> "미세먼지"
            "PM25" -> "초미세먼지"
            else   -> "미세먼지"
        }
        return "$name $grade ${value ?: "-"}㎍/㎥"
    }

    private fun JsonArray.getOrNull(i: Int) = if (i in 0 until size()) get(i) else null
}