package com.github.yun531.weatherapp.domain

import com.github.yun531.weatherapp.data.remote.api.AlertApi
import com.github.yun531.weatherapp.data.remote.dto.AlertEventDto
import com.github.yun531.weatherapp.data.settings.SettingsRepository

class AlertTriggerService(
    private val settingsRepo: SettingsRepository,
    private val alertApi: AlertApi
) {

    data class Result(
        val events: List<AlertEventDto>,
        val skipped: Boolean
    )

    suspend fun executeHourly(regions: List<String>): Result {
        val s = settingsRepo.getOnce()
        if (!s.hourlyEnabled) return Result(emptyList(), skipped = true)

        val events = fetchByKinds(regions, s.enabledKinds, s.warningKinds)
        return Result(events, skipped = false)
    }

    suspend fun executeDaily(regions: List<String>): Result {
        val s = settingsRepo.getOnce()
        if (!s.dailyEnabled) return Result(emptyList(), skipped = true)

        val events = alertApi.getRainForecast(regions)
        return Result(events, skipped = false)
    }

    suspend fun fetchByKinds(
        regions: List<String>,
        kinds: Set<AlertKind>,
        warningKinds: Set<WarningKind> = WarningKind.defaultSet()
    ): List<AlertEventDto> {
        val wkParams = warningKinds.map { it.name }

        return when {
            kinds.contains(AlertKind.RAIN_ONSET) &&
                    kinds.contains(AlertKind.WARNING_ISSUED) ->
                alertApi.getAlertSummary(regions, wkParams)

            kinds.contains(AlertKind.RAIN_ONSET) ->
                alertApi.getRainOnset(regions, maxHour = null)

            kinds.contains(AlertKind.WARNING_ISSUED) ->
                alertApi.getIssuedWarnings(regions, wkParams)

            else -> emptyList()
        }
    }
}