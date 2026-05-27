package com.github.yun531.weatherapp.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.github.yun531.weatherapp.data.region.RegionCatalog
import com.github.yun531.weatherapp.domain.AlertKind
import com.github.yun531.weatherapp.domain.WarningKind
import com.github.yun531.weatherapp.ui.common.ComboBox

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(padding: PaddingValues, vm: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val s by vm.settings.collectAsState()

    val catalog = remember { RegionCatalog.get(context) }

    val chosen = remember(s.region1, s.region2, s.region3) {
        setOf(s.region1, s.region2, s.region3).filter { it.isNotBlank() }.toSet()
    }

    var openSlot by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("설정", style = MaterialTheme.typography.titleLarge)

        // 1) 지역
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column {
                ListItem(
                    headlineContent = { Text("지역") },
                    supportingContent = { Text("최대 3개까지 선택할 수 있습니다") }
                )
                HorizontalDivider()
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RegionSlotRow(
                        label = "지역 1",
                        currentId = s.region1,
                        catalog = catalog,
                        onClick = { openSlot = 1 }
                    )
                    RegionSlotRow(
                        label = "지역 2",
                        currentId = s.region2,
                        catalog = catalog,
                        onClick = { openSlot = 2 }
                    )
                    RegionSlotRow(
                        label = "지역 3",
                        currentId = s.region3,
                        catalog = catalog,
                        onClick = { openSlot = 3 }
                    )
                }
            }
        }

        // 2) 정각 알림
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column {
                ListItem(
                    headlineContent = { Text("정각 알림") },
                    supportingContent = { Text("매 정각에 조건에 맞는 알림을 생성합니다") },
                    trailingContent = {
                        Switch(
                            checked = s.hourlyEnabled,
                            onCheckedChange = { vm.setHourlyEnabled(it) }
                        )
                    }
                )
                HorizontalDivider()
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "생성 항목",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = s.enabledKinds.contains(AlertKind.RAIN_ONSET),
                            onClick = { vm.toggleKind(AlertKind.RAIN_ONSET) },
                            label = { Text("일기예보") },
                            enabled = s.hourlyEnabled
                        )
                        FilterChip(
                            selected = s.enabledKinds.contains(AlertKind.WARNING_ISSUED),
                            onClick = { vm.toggleKind(AlertKind.WARNING_ISSUED) },
                            label = { Text("기상특보") },
                            enabled = s.hourlyEnabled
                        )
                        FilterChip(
                            selected = s.enabledKinds.contains(AlertKind.AIR_POLLUTION),
                            onClick = { vm.toggleKind(AlertKind.AIR_POLLUTION) },
                            label = { Text("미세먼지") },
                            enabled = s.hourlyEnabled
                        )
                    }

                    AnimatedVisibility(
                        visible = s.hourlyEnabled && s.enabledKinds.contains(AlertKind.WARNING_ISSUED)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "특보 종류",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = {
                                        val allSelected = s.warningKinds.size == WarningKind.entries.size
                                        if (allSelected) {
                                            vm.setWarningKindsAll(emptySet())
                                        } else {
                                            vm.setWarningKindsAll(WarningKind.defaultSet())
                                        }
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        if (s.warningKinds.size == WarningKind.entries.size) "전체 해제" else "전체 선택",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }

                            WarningKind.groupedByCategory().forEach { (category, kinds) ->
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(52.dp).padding(top = 6.dp)
                                    )
                                    kinds.forEach { kind ->
                                        FilterChip(
                                            selected = s.warningKinds.contains(kind),
                                            onClick = { vm.toggleWarningKind(kind) },
                                            label = { Text(kind.label, style = MaterialTheme.typography.labelMedium) },
                                            modifier = Modifier.height(28.dp)
                                        )
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

                    if (!s.hourlyEnabled) {
                        Text(
                            "정각 알림을 켜면 항목을 선택할 수 있습니다",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 3) 일일 요약 알림
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column {
                ListItem(
                    headlineContent = { Text("일일 요약 알림") },
                    supportingContent = {
                        Text("알람 시각 기준 24시간 예보 + 7일 예보를 요약해 제공합니다")
                    },
                    trailingContent = {
                        Switch(
                            checked = s.dailyEnabled,
                            onCheckedChange = { vm.setDailyEnabled(it) }
                        )
                    }
                )
                HorizontalDivider()

                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedVisibility(visible = s.dailyEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ComboBox(
                                label = "시각(HH)",
                                options = (0..23).map { "%02d".format(it) to it.toString() },
                                value = s.dailyHour.toString(),
                                onChange = { vm.setDailyHour(it.toIntOrNull() ?: 7) }
                            )

                            Text(
                                "선택한 시각에 일일 요약 알림이 제공됩니다.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AnimatedVisibility(visible = !s.dailyEnabled) {
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

    openSlot?.let { slot ->
        val current = when (slot) {
            1 -> s.region1
            2 -> s.region2
            else -> s.region3
        }
        RegionPickerSheet(
            catalog = catalog,
            excludedIds = chosen - current,
            currentId = current,
            onSelect = { id ->
                vm.setRegion(slot, id)
                openSlot = null
            },
            onDismiss = { openSlot = null }
        )
    }
}