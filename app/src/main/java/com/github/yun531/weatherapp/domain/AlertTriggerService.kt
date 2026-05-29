package com.github.yun531.weatherapp.domain

import com.github.yun531.weatherapp.data.remote.api.AlertApi
import com.github.yun531.weatherapp.data.remote.dto.AlertEventDto
import com.github.yun531.weatherapp.data.settings.SettingsRepository
import com.github.yun531.weatherapp.domain.briefing.BriefingLoader
import com.github.yun531.weatherapp.domain.briefing.RegionBriefing

class AlertTriggerService(
    private val settingsRepo: SettingsRepository,
    private val alertApi: AlertApi,
    private val briefingLoader: BriefingLoader
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

        val briefings = briefingLoader.load(regions, s.enabledKinds, s.warningKinds)
        return TriggerResult.Daily(briefings, skipped = false)
    }

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