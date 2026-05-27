package com.github.yun531.weatherapp.ui.forecast

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.github.yun531.weatherapp.core.ServiceLocator
import com.github.yun531.weatherapp.data.region.RegionCatalog
import com.github.yun531.weatherapp.data.remote.dto.AirQualityDto
import com.github.yun531.weatherapp.domain.AirQualityGrade
import com.github.yun531.weatherapp.domain.Pollutant

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

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

        if (state.loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        // -------- Hourly (가로 스크롤) --------
        val hourly = state.hourly
        if (hourly != null) {
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
        } else {
            SectionPlaceholder("24시간 예보")
        }

        // -------- Daily (ListItem) --------
        val daily = state.daily
        if (daily != null) {
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
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        dayLabel(d.daysAhead, daily.announceTime),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    if (d.daysAhead == 0) {
                                        Text(
                                            "(${koreanDayOfWeek(0, daily.announceTime)})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
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
        } else {
            SectionPlaceholder("7일 예보")
        }

        // 7일 예보 섹션이 끝난 후 미세먼지 섹션 호출
        AirQualitySection(state.airQuality)
    }
}

/**
 * announceTime 기준으로 daysAhead에 해당하는 한글 요일을 반환.
 * 서버 SnapshotAssembler와 동일하게 hour < 6이면 baseDate를 하루 뺀다.
 */
private fun koreanDayOfWeek(daysAhead: Int, announceTime: String): String {
    val baseDate = try {
        val at = LocalDateTime.parse(announceTime)
        if (at.hour < 6) at.toLocalDate().minusDays(1) else at.toLocalDate()
    } catch (_: Exception) {
        val zdt = parseTimeToSeoul(announceTime)
        if (zdt != null && zdt.hour < 6) zdt.toLocalDate().minusDays(1)
        else zdt?.toLocalDate() ?: LocalDate.now()
    }

    val dayOfWeek = baseDate.plusDays(daysAhead.toLong()).dayOfWeek
    return when (dayOfWeek) {
        DayOfWeek.MONDAY    -> "월"
        DayOfWeek.TUESDAY   -> "화"
        DayOfWeek.WEDNESDAY -> "수"
        DayOfWeek.THURSDAY  -> "목"
        DayOfWeek.FRIDAY    -> "금"
        DayOfWeek.SATURDAY  -> "토"
        DayOfWeek.SUNDAY    -> "일"
    }
}

/**
 * daysAhead를 화면 표시용 라벨로 변환.
 * - 0 -> "오늘 (요일)"
 * - 1~6 -> 한글 요일 (월/화/수/목/금/토/일)
 */
private fun dayLabel(daysAhead: Int, announceTime: String): String {
    if (daysAhead == 0) return "오늘"
    return koreanDayOfWeek(daysAhead, announceTime)
}

@Composable
private fun SectionPlaceholder(title: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("정보 없음", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AirQualitySection(airQuality: AirQualityDto?) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            PollutantBlock(
                label = "미세먼지",
                pollutant = Pollutant.PM10,
                value = airQuality?.pm10,
                grade = AirQualityGrade.fromWire(airQuality?.pm10Grade)
            )
            PollutantBlock(
                label = "초미세먼지",
                pollutant = Pollutant.PM25,
                value = airQuality?.pm25,
                grade = AirQualityGrade.fromWire(airQuality?.pm25Grade)
            )
        }
    }
}

@Composable
private fun PollutantBlock(
    label: String,
    pollutant: Pollutant,
    value: Int?,
    grade: AirQualityGrade?
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (value == null || grade == null) {
            Text(
                "정보 없음",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                "${grade.label}(${value}㎍/㎥)",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            AirQualityBar(grade = grade, fraction = pollutant.fillFraction(value))
        }
    }
}

@Composable
private fun AirQualityBar(grade: AirQualityGrade, fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(10.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(gradeColor(grade))
        )
    }
}

private fun gradeColor(grade: AirQualityGrade): Color = when (grade) {
    AirQualityGrade.GOOD     -> Color(0xFF4FC3F7)
    AirQualityGrade.MODERATE -> Color(0xFF66BB6A)
    AirQualityGrade.BAD      -> Color(0xFFFFA726)
    AirQualityGrade.VERY_BAD -> Color(0xFFEF5350)
}