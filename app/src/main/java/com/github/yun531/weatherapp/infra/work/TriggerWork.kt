package com.github.yun531.weatherapp.infra.work

import android.content.Context
import androidx.work.*
import com.github.yun531.weatherapp.domain.TriggerType
import java.util.concurrent.TimeUnit

object TriggerWork {
    private const val KEY_TYPE = "type"
    private const val KEY_TRIGGER_AT = "triggerAtLocal"
    private const val KEY_HOUR = "hour"

    fun enqueue(context: Context, type: TriggerType, triggerAtLocal: String, hour: String) {
        val uniqueName = "trigger_${type.name}_$triggerAtLocal"

        val req = OneTimeWorkRequestBuilder<TriggerFetchWorker>()
            .setInputData(
                workDataOf(
                    KEY_TYPE to type.name,
                    KEY_TRIGGER_AT to triggerAtLocal,
                    KEY_HOUR to hour
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(uniqueName, ExistingWorkPolicy.KEEP, req)
    }

    fun parseType(data: Data): TriggerType? =
        runCatching { TriggerType.valueOf(data.getString(KEY_TYPE) ?: "") }.getOrNull()

    fun triggerAt(data: Data): String = data.getString(KEY_TRIGGER_AT) ?: ""
    fun hour(data: Data): String = data.getString(KEY_HOUR) ?: ""
}