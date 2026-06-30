package com.github.yun531.weatherapp.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.yun531.weatherapp.domain.AlertKind
import com.github.yun531.weatherapp.domain.WarningKind
import com.github.yun531.weatherapp.ui.common.AirIcon
import com.github.yun531.weatherapp.ui.common.BellIcon
import com.github.yun531.weatherapp.ui.common.ClockIcon
import com.github.yun531.weatherapp.ui.common.CloudIcon
import com.github.yun531.weatherapp.ui.common.IconBadge
import com.github.yun531.weatherapp.ui.common.WarningIcon

private const val DISABLED_ALPHA = 0.38f

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HourlyAlertCard(hourlyEnabled: Boolean, enabled: Boolean, vm: SettingsViewModel) {
    SettingsCard {
        AlertToggleRow(
            icon = BellIcon,
            title = "정각 알림",
            subtitle = "매 정각에 조건에 맞는 알림을 생성합니다",
            checked = hourlyEnabled,
            enabled = enabled,
            onCheckedChange = { vm.setHourlyEnabled(it) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AlertKindsCard(
    anyAlertEnabled: Boolean,
    hasRegion: Boolean,
    enabledKinds: Set<AlertKind>,
    warningKinds: Set<WarningKind>,
    vm: SettingsViewModel
) {
    val kindsSelectable = hasRegion && anyAlertEnabled
    SettingsCard {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "생성 항목",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "선택한 항목은 정각 알림과 일일 요약 알림에 공통으로 적용됩니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AlertKindChip(CloudIcon, "일기예보", enabledKinds.contains(AlertKind.RAIN_ONSET), kindsSelectable) {
                    vm.toggleKind(AlertKind.RAIN_ONSET)
                }
                AlertKindChip(WarningIcon, "기상특보", enabledKinds.contains(AlertKind.WARNING_ISSUED), kindsSelectable) {
                    vm.toggleKind(AlertKind.WARNING_ISSUED)
                }
                AlertKindChip(AirIcon, "미세먼지", enabledKinds.contains(AlertKind.AIR_POLLUTION), kindsSelectable) {
                    vm.toggleKind(AlertKind.AIR_POLLUTION)
                }
            }

            AnimatedVisibility(visible = kindsSelectable && enabledKinds.contains(AlertKind.WARNING_ISSUED)) {
                WarningKindPicker(warningKinds = warningKinds, vm = vm)
            }

            if (hasRegion && !anyAlertEnabled) {
                Text(
                    "정각 알림 또는 일일 요약 알림을 켜면 항목을 선택할 수 있습니다",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DailyAlertCard(dailyEnabled: Boolean, dailyHour: Int, enabled: Boolean, vm: SettingsViewModel) {
    SettingsCard {
        Column {
            AlertToggleRow(
                icon = ClockIcon,
                title = "일일 요약 알림",
                subtitle = "알람 시각 기준 24시간 예보 + 7일 예보를 요약해 제공합니다",
                checked = dailyEnabled,
                enabled = enabled,
                onCheckedChange = { vm.setDailyEnabled(it) }
            )
            HorizontalDivider()
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(visible = dailyEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            "알림 시각",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        DailyHourField(hour = dailyHour, enabled = enabled, onHourChange = { vm.setDailyHour(it) })
                        Text(
                            "선택한 시각에 일일 요약 알림이 제공됩니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                AnimatedVisibility(visible = !dailyEnabled) {
                    Text(
                        "일일 요약 알림을 켜면 시각을 선택할 수 있어요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .alpha(if (enabled) 1f else DISABLED_ALPHA),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconBadge(icon)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertKindChip(icon: ImageVector, label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) }
    )
}

@Composable
private fun DailyHourField(hour: Int, enabled: Boolean = true, onHourChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(13.dp)
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else DISABLED_ALPHA)
                .clip(shape)
                .border(1.3.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .clickable(enabled = enabled) { expanded = true }
                .padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "시각 (HH)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(ClockIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(
                    "%02d:00".format(hour),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            (0..23).forEach { h ->
                DropdownMenuItem(
                    text = { Text("%02d:00".format(h)) },
                    onClick = {
                        onHourChange(h)
                        expanded = false
                    }
                )
            }
        }
    }
}