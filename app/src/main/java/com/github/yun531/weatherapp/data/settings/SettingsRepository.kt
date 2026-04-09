package com.github.yun531.weatherapp.data.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.github.yun531.weatherapp.domain.AlertKind
import com.github.yun531.weatherapp.domain.WarningKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "weatherapp_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val REGION1 = stringPreferencesKey("region1")
        val REGION2 = stringPreferencesKey("region2")
        val REGION3 = stringPreferencesKey("region3")

        val HOURLY_ENABLED = booleanPreferencesKey("hourly_enabled")
        val ENABLED_KINDS_CSV = stringPreferencesKey("enabled_kinds_csv")
        val WARNING_KINDS_CSV = stringPreferencesKey("warning_kinds_csv")

        val DAILY_ENABLED = booleanPreferencesKey("daily_enabled")
        val DAILY_HOUR = intPreferencesKey("daily_hour")

        // 마지막으로 “적용했다고 기록한 토픽 목록”
        val APPLIED_TOPICS_CSV = stringPreferencesKey("applied_topics_csv")
        // 마지막으로 알고 있던 FCM 토큰(토큰 변경 감지/복구용)
        val LAST_FCM_TOKEN = stringPreferencesKey("last_fcm_token")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { p ->
        val r1 = p[Keys.REGION1] ?: ""
        val r2 = p[Keys.REGION2] ?: ""
        val r3 = p[Keys.REGION3] ?: ""

        val hourlyEnabled = p[Keys.HOURLY_ENABLED] ?: false
        val kindsCsv = p[Keys.ENABLED_KINDS_CSV] ?: AlertKind.toCsv(AlertKind.defaultSet())
        val kinds = AlertKind.parseCsv(kindsCsv)

        val warningKindsCsv = p[Keys.WARNING_KINDS_CSV] ?: WarningKind.toCsv(WarningKind.defaultSet())
        val warningKinds = WarningKind.parseCsv(warningKindsCsv)

        val dailyEnabled = p[Keys.DAILY_ENABLED] ?: false
        val dailyHour = (p[Keys.DAILY_HOUR] ?: 7).coerceIn(0, 23)

        AppSettings(
            region1 = r1, region2 = r2, region3 = r3,
            hourlyEnabled = hourlyEnabled,
            enabledKinds = kinds,
            warningKinds = warningKinds,
            dailyEnabled = dailyEnabled,
            dailyHour = dailyHour
        )
    }

    suspend fun getOnce(): AppSettings = settingsFlow.first()

    fun selectedRegions(s: AppSettings): List<String> =
        listOf(s.region1, s.region2, s.region3)
            .filter { it.isNotBlank() }
            .distinct()

    suspend fun setRegion(slot: Int, regionId: String) {
        context.dataStore.edit { p ->
            when (slot) {
                1 -> p[Keys.REGION1] = regionId
                2 -> p[Keys.REGION2] = regionId
                3 -> p[Keys.REGION3] = regionId
            }
        }
    }

    suspend fun setHourlyEnabled(enabled: Boolean) {
        context.dataStore.edit { p -> p[Keys.HOURLY_ENABLED] = enabled }
    }

    suspend fun setEnabledKinds(kinds: Set<AlertKind>) {
        context.dataStore.edit { p -> p[Keys.ENABLED_KINDS_CSV] = AlertKind.toCsv(kinds) }
    }

    suspend fun setWarningKinds(kinds: Set<WarningKind>) {
        context.dataStore.edit { p -> p[Keys.WARNING_KINDS_CSV] = WarningKind.toCsv(kinds) }
    }

    suspend fun setDailyEnabled(enabled: Boolean) {
        context.dataStore.edit { p -> p[Keys.DAILY_ENABLED] = enabled }
    }

    suspend fun setDailyHour(hour: Int) {
        context.dataStore.edit { p -> p[Keys.DAILY_HOUR] = hour.coerceIn(0, 23) }
    }

    /**  Topic Sync 전용 저장/조회  */

    suspend fun getAppliedTopics(): Set<String> {
        val p = context.dataStore.data.first()
        val csv = p[Keys.APPLIED_TOPICS_CSV] ?: ""
        return csv.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    suspend fun setAppliedTopics(topics: Set<String>) {
        context.dataStore.edit { p ->
            p[Keys.APPLIED_TOPICS_CSV] = topics.joinToString(",")
        }
    }

    suspend fun clearAppliedTopics() {
        context.dataStore.edit { p ->
            p[Keys.APPLIED_TOPICS_CSV] = ""
        }
    }

    suspend fun getLastFcmToken(): String? {
        val p = context.dataStore.data.first()
        val t = p[Keys.LAST_FCM_TOKEN]?.trim().orEmpty()
        return t.ifBlank { null }
    }

    suspend fun setLastFcmToken(token: String) {
        context.dataStore.edit { p ->
            p[Keys.LAST_FCM_TOKEN] = token
        }
    }
}