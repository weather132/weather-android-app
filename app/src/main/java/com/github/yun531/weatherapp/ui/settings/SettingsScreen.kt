package com.github.yun531.weatherapp.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.github.yun531.weatherapp.data.region.RegionCatalog
import com.github.yun531.weatherapp.domain.AlertKind
import com.github.yun531.weatherapp.ui.widgets.ComboBox

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(padding: PaddingValues, vm: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val s by vm.settings.collectAsState()

    val catalog = remember { RegionCatalog.get(context) }
    val all = remember {
        val base = catalog.allOptions().map { it.name to it.id }
        listOf("선택안함" to "") + base
    }

    val chosen = remember(s.region1, s.region2, s.region3) {
        setOf(s.region1, s.region2, s.region3).filter { it.isNotBlank() }.toSet()
    }

    fun filteredOptions(current: String): List<Pair<String, String>> =
        all.filter { (_, v) -> v.isBlank() || v == current || !chosen.contains(v) }

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
                    ComboBox(
                        label = "지역 1",
                        options = filteredOptions(s.region1),
                        value = s.region1,
                        onChange = { vm.setRegion(1, it) }
                    )
                    ComboBox(
                        label = "지역 2",
                        options = filteredOptions(s.region2),
                        value = s.region2,
                        onChange = { vm.setRegion(2, it) }
                    )
                    ComboBox(
                        label = "지역 3",
                        options = filteredOptions(s.region3),
                        value = s.region3,
                        onChange = { vm.setRegion(3, it) }
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
                    }

                    if (!s.hourlyEnabled) {
                        Text(
                            "정각 알림을 켜면 항목을 선택할 수 있어요.",
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
}