package com.github.yun531.weatherapp.infra.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.yun531.weatherapp.core.ServiceLocator
import com.github.yun531.weatherapp.data.region.RegionCatalog
import com.github.yun531.weatherapp.domain.AlertKind
import com.github.yun531.weatherapp.domain.AlertTriggerService.TriggerResult
import com.github.yun531.weatherapp.domain.TriggerType
import com.github.yun531.weatherapp.infra.notification.NotificationHelper
import com.github.yun531.weatherapp.ui.NavRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TriggerFetchWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val type = TriggerWork.parseType(inputData)
            ?: return@withContext Result.success()

        val s = ServiceLocator.settingsRepo.getOnce()
        val regions = ServiceLocator.settingsRepo.selectedRegions(s)
        if (regions.isEmpty()) return@withContext Result.success()

        try {
            val service = ServiceLocator.alertTriggerService
            val result = when (type) {
                TriggerType.DAILY_TRIGGER -> service.executeDaily(regions)
                TriggerType.HOURLY_TRIGGER -> service.executeHourly(regions)
            }

            when (result) {
                is TriggerResult.Hourly -> handleHourly(result, regions, s.enabledKinds)
                is TriggerResult.Daily -> handleDaily(result, regions)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun handleHourly(
        result: TriggerResult.Hourly,
        regions: List<String>,
        enabledKinds: Set<AlertKind>
    ) {
        if (result.skipped) return

        if (result.events.isEmpty()) {
            val names = regionNames(regions)
            val kindLabel = AlertKind.noEventsLabel(enabledKinds)
            NotificationHelper.showSimple(
                applicationContext,
                "정각 알림 · $names",
                "$names: $kindLabel 없음",
                NavRoutes.FORECAST
            )
            return
        }

        NotificationHelper.showAlertEvents(
            applicationContext,
            "정각 알림 (전체)",
            result.events,
            NavRoutes.FORECAST
        )
    }

    private fun handleDaily(result: TriggerResult.Daily, regions: List<String>) {
        if (result.skipped) return

        if (result.briefings.all { it.isEmpty() }) {
            val names = regionNames(regions)
            NotificationHelper.showSimple(
                applicationContext,
                "일기예보 요약 · $names",
                "$names: 현재 요약할 소식이 없습니다",
                NavRoutes.BRIEFING
            )
            return
        }

        NotificationHelper.showRegionBriefings(
            applicationContext,
            dailyTitle(regions),
            result.briefings,
            NavRoutes.BRIEFING
        )
    }

    private fun dailyTitle(regions: List<String>): String =
        if (regions.size == 1) "일기예보 요약 · ${regionNames(regions)}" else "일기예보 요약"

    private fun regionNames(regions: List<String>): String {
        val catalog = RegionCatalog.get(applicationContext)
        return regions.joinToString(", ") { catalog.nameOf(it) }
    }
}