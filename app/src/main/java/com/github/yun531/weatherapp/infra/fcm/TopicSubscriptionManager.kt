package com.github.yun531.weatherapp.infra.fcm

import android.content.Context
import android.util.Log
import com.github.yun531.weatherapp.data.settings.SettingsRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class TopicSubscriptionManager(context: Context) {

    private val appContext = context.applicationContext
    private val repo = SettingsRepository(appContext)
    private val fm = FirebaseMessaging.getInstance()

    private fun dailyTopic(hour: Int) = "daily_%02d".format(hour.coerceIn(0, 23))

    private suspend fun desiredTopics(): Set<String> {
        val s = repo.getOnce()
        val out = LinkedHashSet<String>(2)
        if (s.hourlyEnabled) out += "hourly"
        if (s.dailyEnabled) out += dailyTopic(s.dailyHour)
        return out
    }

    /**
     * 토큰 변경/갱신 상황에서도 구독유지 되도록 만든 동기화.
     *
     * - forceResubscribe=true 이거나
     * - (현재 토큰 != 저장된 토큰) 이면
     *   => appliedTopics를 무시하고 desiredTopics를 “강제 재구독”
     */
    suspend fun sync(reason: String, forceResubscribe: Boolean, tokenHint: String?) {
        val desired = desiredTopics()

        // 현재 토큰 확보(힌트가 있으면 그걸 우선)
        val currentToken = try {
            tokenHint?.takeIf { it.isNotBlank() } ?: fm.token.await()
        } catch (e: Exception) {
            null
        }

        // 토큰 변경 감지
        val lastToken = repo.getLastFcmToken()
        val tokenChanged = (currentToken != null && currentToken != lastToken)

        val effectiveForce = forceResubscribe || tokenChanged
        if (currentToken != null && currentToken != lastToken) {
            repo.setLastFcmToken(currentToken)
        }

        // appliedTopics 로드(강제 재구독이면 “빈 집합 취급”)
        val applied = if (effectiveForce) emptySet() else repo.getAppliedTopics()

        val toUnsub = applied - desired
        val toSub = if (effectiveForce) desired else (desired - applied)

        Log.d("TOPIC_SYNC", "reason=$reason force=$forceResubscribe tokenChanged=$tokenChanged")
        Log.d("TOPIC_SYNC", "applied=$applied desired=$desired toUnsub=$toUnsub toSub=$toSub")

        // unsubscribe (필요한 것만)
        for (t in toUnsub) {
            fm.unsubscribeFromTopic(t).await()
            Log.d("TOPIC_SYNC", "unsubscribed: $t")
        }

        // subscribe (필요한 것만 / 강제면 desired 전부)
        for (t in toSub) {
            fm.subscribeToTopic(t).await()
            Log.d("TOPIC_SYNC", "subscribed: $t")
        }

        // 성공 시 appliedTopics를 desired로 업데이트
        repo.setAppliedTopics(desired)
        Log.d("TOPIC_SYNC", "appliedTopics updated -> $desired")
    }
}