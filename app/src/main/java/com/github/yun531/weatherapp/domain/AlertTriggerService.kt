package com.github.yun531.weatherapp.domain

import com.github.yun531.weatherapp.data.remote.api.AlertApi
import com.github.yun531.weatherapp.data.remote.api.ForecastApi
import com.github.yun531.weatherapp.data.remote.dto.AlertEventDto
import com.github.yun531.weatherapp.data.remote.dto.RegionAirQualityDto
import com.github.yun531.weatherapp.data.settings.SettingsRepository
import com.github.yun531.weatherapp.domain.briefing.BriefingComposer
import com.github.yun531.weatherapp.domain.briefing.RegionBriefing
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class AlertTriggerService(
    private val settingsRepo: SettingsRepository,
    private val alertApi: AlertApi,
    private val forecastApi: ForecastApi
) {

    sealed interface TriggerResult {
        val skipped: Boolean

        data class Hourly(
            val events: List<AlertEventDto>,
            override val skipped: Boolean
        ) : TriggerResult

        data class Daily(
            val briefings: List<RegionBriefing>,
            override val skipped: Boolean
        ) : TriggerResult
    }

    suspend fun executeHourly(regions: List<String>): TriggerResult.Hourly {
        val s = settingsRepo.getOnce()
        if (!s.hourlyEnabled) return TriggerResult.Hourly(emptyList(), skipped = true)

        val events = fetchByKinds(regions, s.enabledKinds, s.warningKinds)
        return TriggerResult.Hourly(events, skipped = false)
    }

    suspend fun executeDaily(regions: List<String>): TriggerResult.Daily {
        val s = settingsRepo.getOnce()
        if (!s.dailyEnabled) return TriggerResult.Daily(emptyList(), skipped = true)

        val briefings = composeDailyBriefings(regions, s.enabledKinds, s.warningKinds)
        return TriggerResult.Daily(briefings, skipped = false)
    }

    private suspend fun composeDailyBriefings(
        regions: List<String>,
        kinds: Set<AlertKind>,
        warningKinds: Set<WarningKind>
    ): List<RegionBriefing> = coroutineScope {
        val rain = async { if (AlertKind.RAIN_ONSET in kinds) fetchRainForecast(regions) else emptyList() }
        val warnings = async { if (AlertKind.WARNING_ISSUED in kinds) fetchWarnings(regions, warningKinds) else emptyList() }
        val air = async { if (AlertKind.AIR_POLLUTION in kinds) fetchAirQualities(regions) else emptyList() }

        BriefingComposer.compose(regions, rain.await(), warnings.await(), air.await())
    }

    private suspend fun fetchRainForecast(regions: List<String>): List<AlertEventDto> =
        runCatching { alertApi.getRainForecast(regions) }.getOrDefault(emptyList())

    private suspend fun fetchWarnings(
        regions: List<String>,
        warningKinds: Set<WarningKind>
    ): List<AlertEventDto> =
        runCatching { alertApi.getIssuedWarnings(regions, warningKinds.map { it.name }) }
            .getOrDefault(emptyList())

    private suspend fun fetchAirQualities(regions: List<String>): List<RegionAirQualityDto> =
        runCatching {
            forecastApi.getAirQualitiesBatch(regions).takeIf { it.isSuccessful }?.body() ?: emptyList()
        }.getOrDefault(emptyList())

    suspend fun fetchByKinds(
        regions: List<String>,
        kinds: Set<AlertKind>,
        warningKinds: Set<WarningKind> = WarningKind.defaultSet()
    ): List<AlertEventDto> {
        if (kinds.isEmpty()) return emptyList()

        val warningKindParams = warningKinds.map { it.name }
        if (kinds.size == 1) return fetchSingle(regions, kinds.first(), warningKindParams)

        return alertApi.getAlertCombined(regions, warningKindParams)
            .filter { AlertKind.fromWire(it.type) in kinds }
    }

    private suspend fun fetchSingle(
        regions: List<String>,
        kind: AlertKind,
        warningKindParams: List<String>
    ): List<AlertEventDto> = when (kind) {
        AlertKind.RAIN_ONSET     -> alertApi.getRainOnset(regions, maxHour = null)
        AlertKind.WARNING_ISSUED -> alertApi.getIssuedWarnings(regions, warningKindParams)
        AlertKind.AIR_POLLUTION  -> alertApi.getAirPollution(regions)
    }
}