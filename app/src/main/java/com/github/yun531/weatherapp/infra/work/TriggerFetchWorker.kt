package com.github.yun531.weatherapp.infra.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.yun531.weatherapp.core.ServiceLocator
import com.github.yun531.weatherapp.data.region.RegionCatalog
import com.github.yun531.weatherapp.domain.AlertKind
import com.github.yun531.weatherapp.domain.TriggerType
import com.github.yun531.weatherapp.infra.notification.NotificationHelper
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

            Log.d("ALERT", "type=$type events=${result.events.size} skipped=${result.skipped}")

            if (result.skipped) return@withContext Result.success()

            if (result.events.isEmpty()) {
                val (title, body) = when (type) {
                    TriggerType.HOURLY_TRIGGER -> {
                        val catalog = RegionCatalog.get(applicationContext)
                        val names = regions.joinToString(", ") { catalog.nameOf(it) }
                        val kindLabel = AlertKind.noEventsLabel(s.enabledKinds)
                        "정각 알림 · $names" to "$names: $kindLabel 없음"
                    }
                    TriggerType.DAILY_TRIGGER -> {
                        val catalog = RegionCatalog.get(applicationContext)
                        val names = regions.joinToString(", ") { catalog.nameOf(it) }
                        "일기예보 요약 · $names" to "$names: 현재 요약할 비 소식이 없습니다"
                    }
                }
                NotificationHelper.showSimple(applicationContext, title, body)
                return@withContext Result.success()
            }

            val title = when (type) {
                TriggerType.DAILY_TRIGGER -> "일기예보 요약"
                TriggerType.HOURLY_TRIGGER -> "정각 알림 (전체)"
            }
            NotificationHelper.showAlertEvents(applicationContext, title, result.events)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}