package com.github.yun531.weatherapp.infra.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
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
import java.time.DayOfWeek
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

    // ==================== 브리핑 표시 상수 ====================
    // announceTime 이 없는 fallback 경로에서만 사용하는 절대 시각 포매터
    private val BRIEFING_DATE_HOUR = DateTimeFormatter.ofPattern("M/d HH시")
    private val BRIEFING_HOUR_ONLY = DateTimeFormatter.ofPattern("HH시")

    // 제목에 덧붙는 작은 글씨 날짜
    private val TITLE_DATE = DateTimeFormatter.ofPattern("M/d")
    private const val TITLE_DATE_SCALE = 0.8f

    private const val INDENT = "  "
    private const val BULLET = "· "
    private const val REGION_RULE = "─────"

    private const val CATEGORY_RAIN = "비"
    private const val CATEGORY_WARNING = "기상특보"
    private const val CATEGORY_AIR = "대기질"

    private const val LABEL_PM10 = "미세먼지"
    private const val LABEL_PM25 = "초미세먼지"

    private const val EMPTY_REGION_LINE = "특이사항 없음"

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
     * 일일 요약/브리핑 표시 로직.
     *
     * 표현 규칙:
     * - 카테고리 헤더([비]/[대기질]/[기상특보]) + 들여쓴 하위 항목으로 그룹화.
     * - 비는 시간 구간 + 일자별(오전/오후) 전망을 항목으로 전개. 날짜는 '오늘/내일/모레/M/d(요일)' 상대 표기.
     * - 다지역이면 각 지역을 "─────[지역]─────" 규칙선으로 구분, 단일 지역이면 생략(제목이 지역명을 담음).
     * - 제목에는 기준일을 작은 글씨 "(M/d)" 로 덧붙여 본문의 '오늘'을 고정.
     * - BigTextStyle 로 줄 수 제한 없이 표시.
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
        val body = buildBriefingBody(briefings, catalog)
        if (body.isBlank()) return

        val baseDate = briefingBaseDate(briefings) ?: LocalDate.now()
        val collapsed = collapsedSummary(briefings, catalog) ?: title

        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(styledTitle(title, baseDate))
            .setContentText(collapsed)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .build()

        nm.notify(Random.nextInt(Int.MAX_VALUE), n)
    }

    private data class CategoryBlock(val header: String, val items: List<String>)

    private fun styledTitle(baseTitle: String, date: LocalDate): CharSequence {
        val datePart = " (${date.format(TITLE_DATE)})"
        val builder = SpannableStringBuilder(baseTitle).append(datePart)
        builder.setSpan(
            RelativeSizeSpan(TITLE_DATE_SCALE),
            baseTitle.length,
            builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return builder
    }

    /** 본문/제목 기준일: 첫 비 브리핑의 발표일. 비가 없으면 호출부에서 디바이스 오늘로 대체. */
    private fun briefingBaseDate(briefings: List<RegionBriefing>): LocalDate? =
        briefings.firstNotNullOfOrNull { it.rain?.announceTime?.toLocalDate() }

    private fun buildBriefingBody(briefings: List<RegionBriefing>, catalog: RegionCatalog): String {
        val multiRegion = briefings.size > 1
        return briefings.joinToString("\n\n") { regionBlock(it, catalog, multiRegion) }
    }

    private fun regionBlock(b: RegionBriefing, catalog: RegionCatalog, withRule: Boolean): String {
        val ruleLines =
            if (withRule) listOf("$REGION_RULE[${catalog.nameOf(b.regionId)}]$REGION_RULE")
            else emptyList()
        val categories = briefingCategories(b)
        val contentLines =
            if (categories.isEmpty()) listOf("$INDENT$EMPTY_REGION_LINE")
            else categories.flatMap { categoryLines(it) }
        return (ruleLines + contentLines).joinToString("\n")
    }

    private fun categoryLines(cat: CategoryBlock): List<String> =
        listOf("[${cat.header}]") + cat.items.map { "$INDENT$BULLET$it" }

    private fun briefingCategories(b: RegionBriefing): List<CategoryBlock> =
        listOfNotNull(
            rainCategory(b.rain),
            warningCategory(b.warnings),
            airCategory(b.air)
        )

    // ==================== 비 ====================

    private fun rainCategory(rain: RainBriefing?): CategoryBlock? {
        if (rain == null || !rain.hasAnyRain()) return null
        val items = rainIntervalItems(rain) + rainDayItems(rain)
        if (items.isEmpty()) return null
        return CategoryBlock(CATEGORY_RAIN, items)
    }

    private fun rainIntervalItems(rain: RainBriefing): List<String> {
        val base = rain.announceTime?.toLocalDate()
        return rain.intervals.map { intervalLabel(it, base) }
    }

    private fun intervalLabel(iv: RainBriefing.RainInterval, base: LocalDate?): String {
        if (base == null) return absoluteIntervalLabel(iv)

        val startLabel = relativeDayLabel(iv.start.toLocalDate(), base)
        if (iv.start.toLocalDate() == iv.end.toLocalDate()) {
            return "$startLabel ${iv.start.hour}시~${iv.end.hour}시"
        }
        val endLabel = relativeDayLabel(iv.end.toLocalDate(), base)
        return "$startLabel ${iv.start.hour}시~$endLabel ${iv.end.hour}시"
    }

    private fun absoluteIntervalLabel(iv: RainBriefing.RainInterval): String {
        val sameDay = iv.start.toLocalDate() == iv.end.toLocalDate()
        val end = if (sameDay) iv.end.format(BRIEFING_HOUR_ONLY) else iv.end.format(BRIEFING_DATE_HOUR)
        return "${iv.start.format(BRIEFING_DATE_HOUR)}~$end"
    }

    private fun rainDayItems(rain: RainBriefing): List<String> {
        val baseDate = rain.announceTime?.toLocalDate() ?: return emptyList()
        return rain.days.mapIndexedNotNull { offset, day -> rainDayItem(baseDate, offset, day) }
    }

    private fun rainDayItem(baseDate: LocalDate, offset: Int, day: RainBriefing.DayRain): String? {
        val period = rainPeriodLabel(day) ?: return null
        return "${dayOffsetLabel(baseDate, offset)} $period"
    }

    private fun rainPeriodLabel(day: RainBriefing.DayRain): String? = when {
        day.rainAm && day.rainPm -> "오전·오후"
        day.rainAm -> "오전"
        day.rainPm -> "오후"
        else -> null
    }

    private fun relativeDayLabel(date: LocalDate, base: LocalDate): String =
        dayOffsetLabel(base, ChronoUnit.DAYS.between(base, date).toInt())

    private fun dayOffsetLabel(baseDate: LocalDate, offset: Int): String = when (offset) {
        0 -> "오늘"
        1 -> "내일"
        2 -> "모레"
        else -> baseDate.plusDays(offset.toLong()).let {
            "${it.monthValue}/${it.dayOfMonth}(${weekdayLabel(it.dayOfWeek)})"
        }
    }

    private fun weekdayLabel(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
        DayOfWeek.MONDAY -> "월"
        DayOfWeek.TUESDAY -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY -> "목"
        DayOfWeek.FRIDAY -> "금"
        DayOfWeek.SATURDAY -> "토"
        DayOfWeek.SUNDAY -> "일"
    }

    // ==================== 기상특보 ====================

    private fun warningCategory(warnings: List<WarningBriefing>): CategoryBlock? {
        if (warnings.isEmpty()) return null
        val items = warnings.map { "${kindLabel(it.kind)} ${levelLabel(it.level)}" }
        return CategoryBlock(CATEGORY_WARNING, items)
    }

    // ==================== 대기질 ====================

    private fun airCategory(air: AirBriefing?): CategoryBlock? {
        if (air == null || !air.hasAnyMeasurement()) return null
        val items = airItems(air)
        if (items.isEmpty()) return null
        return CategoryBlock(CATEGORY_AIR, items)
    }

    private fun airItems(air: AirBriefing): List<String> {
        val items = mutableListOf<String>()
        air.pm10Grade?.let { items += pmItem(LABEL_PM10, it, air.pm10, air.pm10NeedsValue()) }
        air.pm25Grade?.let { items += pmItem(LABEL_PM25, it, air.pm25, air.pm25NeedsValue()) }
        return items
    }

    private fun pmItem(label: String, grade: String, value: Int?, withValue: Boolean): String =
        if (withValue && value != null) "$label ${gradeLabel(grade)} ${value}㎍/㎥"
        else "$label ${gradeLabel(grade)}"

    // ==================== 접힌 상태 1줄 요약 ====================

    private fun collapsedSummary(briefings: List<RegionBriefing>, catalog: RegionCatalog): String? {
        val multiRegion = briefings.size > 1
        for (b in briefings) {
            val firstCategory = briefingCategories(b).firstOrNull() ?: continue
            val firstItem = firstCategory.items.firstOrNull() ?: continue
            val prefix = if (multiRegion) "[${catalog.nameOf(b.regionId)}] " else ""
            return "$prefix${firstCategory.header} $firstItem"
        }
        return null
    }

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