package com.github.yun531.weatherapp.domain.briefing

import com.github.yun531.weatherapp.data.remote.dto.AlertEventDto
import com.github.yun531.weatherapp.data.remote.dto.RegionAirQualityDto
import com.google.gson.JsonObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 비/특보/미세먼지 세 소스를 regionId 기준으로 RegionBriefing 으로 병합.
 * 순수 변환(부수효과 없음).
 */
object BriefingComposer {

    fun compose(
        regions: List<String>,
        rainEvents: List<AlertEventDto>,
        warningEvents: List<AlertEventDto>,
        airViews: List<RegionAirQualityDto>
    ): List<RegionBriefing> {
        val rainByRegion = rainEvents.associateBy { it.regionId }
        val warningsByRegion = warningEvents.groupBy { it.regionId }
        val airByRegion = airViews.associateBy { it.regionId }

        return regions.map { regionId ->
            RegionBriefing(
                regionId = regionId,
                rain = rainByRegion[regionId]?.let { toRainBriefing(it) },
                warnings = warningsByRegion[regionId].orEmpty().map { toWarningBriefing(it.payload) },
                air = airByRegion[regionId]?.let { toAirBriefing(it) }
            )
        }
    }

    private fun toRainBriefing(event: AlertEventDto): RainBriefing =
        RainBriefing(
            intervals = parseIntervals(event.payload),
            days = parseDays(event.payload),
            announceTime = parseDateTime(event.occurredAt)
        )

    private fun parseIntervals(payload: JsonObject): List<RainBriefing.RainInterval> {
        val arr = payload.getAsJsonArray("hourlyParts") ?: return emptyList()
        return arr.mapNotNull { el ->
            val o = el.asJsonObject
            val start = parseDateTime(o.get("start")?.asString) ?: return@mapNotNull null
            val end = parseDateTime(o.get("end")?.asString) ?: start
            RainBriefing.RainInterval(start, end)
        }
    }

    private fun parseDays(payload: JsonObject): List<RainBriefing.DayRain> {
        val arr = payload.getAsJsonArray("dayParts") ?: return emptyList()
        return arr.map { el ->
            val o = el.asJsonObject
            RainBriefing.DayRain(
                rainAm = o.get("rainAm")?.asBoolean ?: false,
                rainPm = o.get("rainPm")?.asBoolean ?: false
            )
        }
    }

    private fun toWarningBriefing(payload: JsonObject): WarningBriefing =
        WarningBriefing(
            kind = payload.get("kind")?.asString.orEmpty(),
            level = payload.get("level")?.asString.orEmpty(),
            eventType = payload.get("eventType")?.asString.orEmpty()
        )

    private fun toAirBriefing(dto: RegionAirQualityDto): AirBriefing =
        AirBriefing(
            pm10 = dto.view.pm10,
            pm10Grade = dto.view.pm10Grade,
            pm25 = dto.view.pm25,
            pm25Grade = dto.view.pm25Grade
        )

    private fun parseDateTime(raw: String?): LocalDateTime? =
        raw?.let { runCatching { LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }.getOrNull() }
}