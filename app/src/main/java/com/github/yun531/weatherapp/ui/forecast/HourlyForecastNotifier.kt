package com.github.yun531.weatherapp.ui.forecast

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.yun531.weatherapp.R
import com.github.yun531.weatherapp.data.remote.dto.HourlyForecastDto
import kotlin.math.absoluteValue

object HourlyForecastNotifier {

    private const val CHANNEL_ID = "forecast_hourly_24h"
    private const val CHANNEL_NAME = "예보 알림"
    private const val CHANNEL_DESC = "24시간 예보 알림"

    fun show(context: Context, regionId: String, regionName: String, hourly: HourlyForecastDto) {
        ensureChannel(context)

        val lines = hourly.hours.take(24).map { p ->
            val label = formatValidAtLabel(hourly.reportTime, p.validAt)
            "$label  ${p.temp}°  강수 ${p.pop}%"
        }

        // 1) apply 대신 "명시적 생성 → 명시적 호출"
        val inbox = NotificationCompat.InboxStyle()
        for (line in lines) {
            inbox.addLine(line)
        }
        inbox.setSummaryText("업데이트: ${hourly.reportTime}")

        val notificationId = (100_000 + regionId.hashCode().absoluteValue)

        // 2) Java 메서드에는 named argument 쓰지 말고 위치 인자로 호출
        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$regionName 24시간 예보")
            .setContentText("업데이트: ${hourly.reportTime}")
            .setStyle(inbox)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // 3) notify도 named argument 금지 (Java 메서드)
        NotificationManagerCompat.from(context).notify(notificationId, n)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return

        val ch = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESC
        }
        nm.createNotificationChannel(ch)
    }
}