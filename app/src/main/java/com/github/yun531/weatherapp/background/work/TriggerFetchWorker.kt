package com.github.yun531.weatherapp.background.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.yun531.weatherapp.core.ServiceLocator
import com.github.yun531.weatherapp.domain.AlertKind
import com.github.yun531.weatherapp.domain.TriggerType
import com.github.yun531.weatherapp.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TriggerFetchWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val type = TriggerWork.parseType(inputData) ?: return@withContext Result.success()

        val s = ServiceLocator.settingsRepo.getOnce()
        val regions = ServiceLocator.settingsRepo.selectedRegions(s)
        if (regions.isEmpty()) return@withContext Result.success()

        try {
            when (type) {
                TriggerType.DAILY_TRIGGER -> {
                    if (!s.dailyEnabled) return@withContext Result.success()

                    // API 호출
                    val events = ServiceLocator.alertApi.getClimateDay(regions)

                    // 디버그 로그(요청대로: API 호출 직후)
                    Log.d("ALERT", "type=$type events=${events.size}")

                    // 디버그용: 빈 이벤트여도 알림을 1개 띄워 파이프라인만 검증
                    if (events.isEmpty()) {
                        NotificationHelper.showSimple(
                            applicationContext,
                            "테스트",
                            "events=0 (정상: 알림 조건 미충족)"
                        )
                        return@withContext Result.success()
                    }

                    NotificationHelper.showAlertEvents(applicationContext, "일기예보 요약", events)
                }

                TriggerType.HOURLY_TRIGGER -> {
                    if (!s.hourlyEnabled) return@withContext Result.success()

                    val kinds = s.enabledKinds

                    // API 호출(조건별)
                    val events = when {
                        kinds.contains(AlertKind.RAIN_ONSET) && kinds.contains(AlertKind.WARNING_ISSUED) ->
                            ServiceLocator.alertApi.getSummary(regions)

                        kinds.contains(AlertKind.RAIN_ONSET) ->
                            ServiceLocator.alertApi.getClimate3Hour(regions, maxHour = null)

                        kinds.contains(AlertKind.WARNING_ISSUED) ->
                            ServiceLocator.alertApi.getWarning(regions)

                        else -> emptyList()
                    }

                    // 디버그 로그(요청대로: API 호출/결정 직후)
                    Log.d("ALERT", "type=$type events=${events.size}")

                    // 디버그용: 빈 이벤트여도 알림을 1개 띄워 파이프라인만 검증
                    if (events.isEmpty()) {
                        NotificationHelper.showSimple(
                            applicationContext,
                            "테스트",
                            "events=0 (정상: 알림 조건 미충족)"
                        )
                        return@withContext Result.success()
                    }

                    NotificationHelper.showAlertEvents(applicationContext, "정각 알림 (전체)", events)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}