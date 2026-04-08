package com.github.yun531.weatherapp.ui.forecast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.github.yun531.weatherapp.core.ServiceLocator
import com.github.yun531.weatherapp.data.region.RegionCatalog

@Composable
fun ForecastScreen(padding: PaddingValues, vm: ForecastViewModel = viewModel()) {
    val settings by ServiceLocator.settingsRepo.settingsFlow.collectAsState(initial = null)
    val regions = remember(settings) {
        settings?.let { ServiceLocator.settingsRepo.selectedRegions(it) } ?: emptyList()
    }

    Box(Modifier.padding(padding).fillMaxSize()) {
        if (regions.isEmpty()) {
            Text("설정에서 지역을 선택해주세요.", modifier = Modifier.padding(16.dp))
            return@Box
        }

        val pagerState = rememberPagerState(pageCount = { regions.size })

        LaunchedEffect(pagerState.currentPage, regions) {
            val regionId = regions.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
            vm.loadIfNeeded(regionId)
        }

        Column(Modifier.fillMaxSize()) {
            Text(
                "지역 ${pagerState.currentPage + 1}/${regions.size}",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                ForecastPage(regionId = regions[page], vm = vm)
            }
        }
    }
}

@Composable
private fun ForecastPage(regionId: String, vm: ForecastViewModel) {
    val stateMap by vm.stateByRegion.collectAsState()
    val state = stateMap[regionId] ?: ForecastUiState()

    val catalog = RegionCatalog.get(ServiceLocator.appContext)
    val regionName = catalog.nameOf(regionId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(regionName, style = MaterialTheme.typography.titleLarge)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { vm.refresh(regionId) }) { Text("새로고침") }

                Box {
                    var menuExpanded by remember { mutableStateOf(false) }

                    TextButton(
                        onClick = { menuExpanded = true },
                        enabled = !state.loading
                    ) { Text("알림 생성") }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("예보 알림 생성") },
                            onClick = {
                                menuExpanded = false
                                vm.runHourlyTriggerNowByButton(regionId, regionName)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("요약 알림 생성") },
                            onClick = {
                                menuExpanded = false
                                vm.runDailyTriggerNowByButton(regionId, regionName)
                            }
                        )
                    }
                }
            }
        }

        if (state.warnings.isNotEmpty()) {
            WarningBanner(warnings = state.warnings)
        }

        when {
            state.loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error != null -> Text("에러: ${state.error}")
        }

        // -------- Hourly (가로 스크롤) --------
        state.hourly?.let { hourly ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("24시간 예보", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "업데이트: ${hourly.announceTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    HourlyTempChart(
                        points = hourly.hourlyPoints,
                        announceTime = hourly.announceTime
                    )

                    Spacer(Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        itemsIndexed(hourly.hourlyPoints) { _, p ->
                            ElevatedCard(
                                modifier = Modifier.widthIn(min = 96.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = formatValidAtLabel(hourly.announceTime, p.effectiveTime),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${p.temp}°",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "강수 ${p.pop}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // -------- Daily (ListItem) --------
        state.daily?.let { daily ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("7일 예보", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "업데이트: ${daily.announceTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    daily.dailyPoints.forEachIndexed { index, d ->
                        ListItem(
                            headlineContent = {
                                Text("D+${d.daysAhead}", style = MaterialTheme.typography.titleSmall)
                            },
                            supportingContent = {
                                Text(
                                    "최저 ${d.minTemp}° · 최고 ${d.maxTemp}°",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "오전 ${d.amPop}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "오후 ${d.pmPop}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        )

                        if (index != daily.dailyPoints.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}