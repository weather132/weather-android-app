package com.github.yun531.weatherapp.infra.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.github.yun531.weatherapp.infra.fcm.TopicSubscriptionManager
import java.util.concurrent.TimeUnit

class TopicSyncWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val reason = inputData.getString(KEY_REASON) ?: "UNKNOWN"
        val force = inputData.getBoolean(KEY_FORCE, false)
        val tokenHint = inputData.getString(KEY_TOKEN)

        return try {
            TopicSubscriptionManager(applicationContext).sync(
                reason = reason,
                forceResubscribe = force,
                tokenHint = tokenHint
            )
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val UNIQUE_NAME = "topic_sync"
        private const val KEY_REASON = "reason"
        private const val KEY_FORCE = "force"
        private const val KEY_TOKEN = "token"

        fun enqueue(
            context: Context,
            reason: String,
            forceResubscribe: Boolean = false,
            tokenHint: String? = null
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = workDataOf(
                KEY_REASON to reason,
                KEY_FORCE to forceResubscribe,
                KEY_TOKEN to (tokenHint ?: "")
            )

            val req = OneTimeWorkRequestBuilder<TopicSyncWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    10,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context.applicationContext)
                // 여러 번 호출돼도 마지막 요청만 남기기(REPLACE)
                .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.REPLACE, req)
        }
    }
}