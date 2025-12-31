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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.github.yun531.weatherapp.core.ServiceLocator
import com.github.yun531.weatherapp.data.region.RegionCatalog

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Locale

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

        // 현재 페이지에 들어왔을 때만 로드
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
            TextButton(onClick = { vm.refresh(regionId) }) { Text("새로고침") }
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
                        "업데이트: ${hourly.reportTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        itemsIndexed(hourly.hours) { _, p ->
                            ElevatedCard(
                                modifier = Modifier.widthIn(min = 96.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = formatHourLabel(hourly.reportTime, p.hourOffset),
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
                        "업데이트: ${daily.reportTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    daily.days.forEachIndexed { index, d ->
                        ListItem(
                            headlineContent = {
                                Text("D+${d.dayOffset}", style = MaterialTheme.typography.titleSmall)
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

                        if (index != daily.days.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

/**
 * reportTime(기준 시각) + hourOffset(시간 오프셋)을 이용해 "20시", "내일 01시"처럼 표시
 * reportTime 포맷이 파싱 불가하면 "+Nh"로 폴백
 */
private fun formatHourLabel(reportTime: String, hourOffset: Int): String {
    val base = parseReportTimeToSeoul(reportTime) ?: return "+${hourOffset}h"
    val t = base.plusHours(hourOffset.toLong())

    val hourText = String.format(Locale.KOREA, "%02d시", t.hour)

    val dayDiff = ChronoUnit.DAYS.between(base.toLocalDate(), t.toLocalDate())
    return when (dayDiff) {
        0L -> hourText
        1L -> "내일 $hourText"
        2L -> "모레 $hourText"
        else -> "${t.monthValue}/${t.dayOfMonth} $hourText"
    }
}

/**
 * reportTime 문자열을 Asia/Seoul 기준 ZonedDateTime으로 파싱
 * - ISO(OffsetDateTime/Instant)
 * - epoch sec/ms
 * - 커스텀 패턴들
 */
private fun parseReportTimeToSeoul(reportTime: String): ZonedDateTime? {
    val zone = ZoneId.of("Asia/Seoul")
    val s = reportTime.trim()

    // 1) ISO-8601 OffsetDateTime (예: 2025-12-31T20:00:00+09:00)
    try {
        return OffsetDateTime.parse(s).atZoneSameInstant(zone)
    } catch (_: Exception) {}

    // 2) ISO-8601 Instant (예: 2025-12-31T11:00:00Z)
    try {
        return Instant.parse(s).atZone(zone)
    } catch (_: Exception) {}

    // 3) 숫자만: epoch seconds(10) / millis(13)
    if (s.isNotEmpty() && s.all { it.isDigit() }) {
        val v = runCatching { s.toLong() }.getOrNull() ?: return null
        return when (s.length) {
            13 -> Instant.ofEpochMilli(v).atZone(zone)
            10 -> Instant.ofEpochSecond(v).atZone(zone)
            else -> null
        }
    }

    // 4) 커스텀 패턴
    val patterns = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd HH",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm",
        "yyyyMMddHHmmss",
        "yyyyMMddHHmm",
        "yyyyMMddHH"
    )

    for (p in patterns) {
        val fmt = DateTimeFormatter.ofPattern(p)
        try {
            val ldt = LocalDateTime.parse(s, fmt)
            return ldt.atZone(zone)
        } catch (_: DateTimeParseException) {
        }
    }

    return null
}