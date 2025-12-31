package com.github.yun531.weatherapp.data.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.github.yun531.weatherapp.domain.AlertKind
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

        val DAILY_ENABLED = booleanPreferencesKey("daily_enabled")
        val DAILY_HOUR = intPreferencesKey("daily_hour")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { p ->
        val r1 = p[Keys.REGION1] ?: ""
        val r2 = p[Keys.REGION2] ?: ""
        val r3 = p[Keys.REGION3] ?: ""

        val hourlyEnabled = p[Keys.HOURLY_ENABLED] ?: false
        val kindsCsv = p[Keys.ENABLED_KINDS_CSV] ?: AlertKind.toCsv(AlertKind.defaultSet())
        val kinds = AlertKind.parseCsv(kindsCsv)

        val dailyEnabled = p[Keys.DAILY_ENABLED] ?: false
        val dailyHour = (p[Keys.DAILY_HOUR] ?: 7).coerceIn(0, 23)

        AppSettings(
            region1 = r1, region2 = r2, region3 = r3,
            hourlyEnabled = hourlyEnabled,
            enabledKinds = kinds,
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

    suspend fun setDailyEnabled(enabled: Boolean) {
        context.dataStore.edit { p -> p[Keys.DAILY_ENABLED] = enabled }
    }

    suspend fun setDailyHour(hour: Int) {
        context.dataStore.edit { p -> p[Keys.DAILY_HOUR] = hour.coerceIn(0, 23) }
    }
}