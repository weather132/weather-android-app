package com.github.yun531.weatherapp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.github.yun531.weatherapp.domain.WarningKind
import com.github.yun531.weatherapp.ui.common.AirIcon
import com.github.yun531.weatherapp.ui.common.DotsIcon
import com.github.yun531.weatherapp.ui.common.RainDropIcon
import com.github.yun531.weatherapp.ui.common.ThermometerIcon

private const val CATEGORY_TEMPERATURE = "기온"
private const val CATEGORY_PRECIPITATION = "강수"
private const val CATEGORY_WIND_OCEAN = "바람·해양"

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WarningKindPicker(warningKinds: Set<WarningKind>, vm: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("특보 종류", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val allSelected = warningKinds.size == WarningKind.entries.size
            TextButton(
                onClick = {
                    if (allSelected) vm.setWarningKindsAll(emptySet())
                    else vm.setWarningKindsAll(WarningKind.defaultSet())
                },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(if (allSelected) "전체 해제" else "전체 선택", style = MaterialTheme.typography.labelMedium)
            }
        }

        WarningKind.groupedByCategory().forEach { (category, kinds) ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.width(84.dp).padding(top = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        categoryIcon(category),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FlowRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    kinds.forEach { kind ->
                        FilterChip(
                            selected = warningKinds.contains(kind),
                            onClick = { vm.toggleWarningKind(kind) },
                            label = { Text(kind.label, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }
        }

        Text(
            "선택한 종류의 기상특보만 알림으로 받습니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun categoryIcon(category: String): ImageVector = when (category) {
    CATEGORY_TEMPERATURE -> ThermometerIcon
    CATEGORY_PRECIPITATION -> RainDropIcon
    CATEGORY_WIND_OCEAN -> AirIcon
    else -> DotsIcon
}