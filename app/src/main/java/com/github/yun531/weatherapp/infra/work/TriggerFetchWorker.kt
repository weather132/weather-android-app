package com.github.yun531.weatherapp.infra.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.yun531.weatherapp.core.ServiceLocator
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
                NotificationHelper.showSimple(
                    applicationContext, "테스트", "events=0 (정상: 알림 조건 미충족)"
                )
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