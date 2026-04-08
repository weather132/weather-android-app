package com.github.yun531.weatherapp.ui.forecast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.yun531.weatherapp.data.remote.dto.AlertEventDto
import com.github.yun531.weatherapp.domain.WarningLabels
import com.google.gson.JsonObject

@Composable
fun WarningBanner(warnings: List<AlertEventDto>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    "기상특보 발효 중",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            warnings.forEach { event ->
                Text(
                    text = formatWarningLabel(event.payload),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

private fun formatWarningLabel(payload: JsonObject): String {
    val kind = payload.get("kind")?.asString
    val level = payload.get("level")?.asString
    val eventType = payload.get("eventType")?.asString

    val kindLabel = WarningLabels.kindLabel(kind)
    val levelLabel = WarningLabels.levelLabel(level)

    return when (eventType) {
        "NEW"        -> "$kindLabel $levelLabel 발령"
        "UPGRADED"   -> "$kindLabel $levelLabel 격상"
        "DOWNGRADED" -> "$kindLabel $levelLabel 하향"
        "EXTENDED"   -> "$kindLabel $levelLabel 연장"
        else         -> "$kindLabel $levelLabel"
    }
}