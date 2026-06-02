package com.github.yun531.weatherapp.ui.forecast

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.yun531.weatherapp.core.ServiceLocator
import com.github.yun531.weatherapp.data.region.RegionCatalog
import com.github.yun531.weatherapp.data.remote.dto.AirQualityDto
import com.github.yun531.weatherapp.data.remote.dto.DailyForecastDto
import com.github.yun531.weatherapp.data.remote.dto.HourlyForecastDto
import com.github.yun531.weatherapp.domain.AirQualityGrade
import com.github.yun531.weatherapp.domain.Pollutant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

private const val RAIN_POP_THRESHOLD = 60

private val RainEmphasis = Color(0xFF1976D2)
private val TempLow = Color(0xFF4FA3E0)
private val TempHigh = Color(0xFFFF9F45)
private val HeroGradient = listOf(Color(0xFF3D7CEC), Color(0xFF5C9BF0), Color(0xFF83B6F4))

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
            regions.getOrNull(pagerState.currentPage)?.let { vm.loadIfNeeded(it) }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            ForecastPage(
                regionId = regions[page],
                pageIndex = page,
                pageCount = regions.size,
                vm = vm
            )
        }
    }
}

@Composable
private fun ForecastPage(regionId: String, pageIndex: Int, pageCount: Int, vm: ForecastViewModel) {
    val stateMap by vm.stateByRegion.collectAsState()
    val state = stateMap[regionId] ?: ForecastUiState()
    val regionName = RegionCatalog.get(ServiceLocator.appContext).nameOf(regionId)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ForecastTopBar(
            pageIndex = pageIndex,
            pageCount = pageCount,
            regionName = regionName,
            loading = state.loading,
            onRefresh = { vm.refresh(regionId) },
            onCreateHourly = { vm.runHourlyTriggerNowByButton(regionId, regionName) },
            onCreateDaily = { vm.runDailyTriggerNowByButton(regionId, regionName) }
        )
        PagerDots(pageIndex, pageCount)

        if (state.warnings.isNotEmpty()) {
            WarningBanner(warnings = state.warnings)
        }
        if (state.loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        CurrentConditionHero(hourly = state.hourly, daily = state.daily)

        val hourly = state.hourly
        if (hourly != null && hourly.hourlyPoints.isNotEmpty()) {
            HourlyForecastCard(hourly)
        } else {
            SectionPlaceholder("24시간 예보")
        }

        val daily = state.daily
        if (daily != null && daily.dailyPoints.isNotEmpty()) {
            WeeklyForecastCard(daily)
        } else {
            SectionPlaceholder("7일 예보")
        }

        AirQualitySection(state.airQuality)
    }
}

// ==================== Top bar ====================

@Composable
private fun ForecastTopBar(
    pageIndex: Int,
    pageCount: Int,
    regionName: String,
    loading: Boolean,
    onRefresh: () -> Unit,
    onCreateHourly: () -> Unit,
    onCreateDaily: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "지역 ${pageIndex + 1} / $pageCount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                regionName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        FilledTonalIconButton(onClick = onRefresh) {
            Icon(Icons.Filled.Refresh, contentDescription = "새로고침")
        }
        Spacer(Modifier.width(6.dp))
        AlertCreateButton(loading, onCreateHourly, onCreateDaily)
    }
}

@Composable
private fun AlertCreateButton(loading: Boolean, onCreateHourly: () -> Unit, onCreateDaily: () -> Unit) {
    Box {
        var expanded by remember { mutableStateOf(false) }

        FilledTonalIconButton(onClick = { expanded = true }, enabled = !loading) {
            Icon(Icons.Filled.Notifications, contentDescription = "알림 생성")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("예보 알림 생성") },
                onClick = { expanded = false; onCreateHourly() }
            )
            DropdownMenuItem(
                text = { Text("요약 알림 생성") },
                onClick = { expanded = false; onCreateDaily() }
            )
        }
    }
}

@Composable
private fun PagerDots(pageIndex: Int, pageCount: Int) {
    if (pageCount <= 1) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { i ->
            val selected = i == pageIndex
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .height(6.dp)
                    .width(if (selected) 18.dp else 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
            )
        }
    }
}

// ==================== Hero ====================

@Composable
private fun CurrentConditionHero(hourly: HourlyForecastDto?, daily: DailyForecastDto?) {
    val currentTemp = hourly?.hourlyPoints?.firstOrNull()?.temp ?: return
    val today = daily?.dailyPoints?.firstOrNull { it.daysAhead == 0 }
        ?: daily?.dailyPoints?.firstOrNull()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(HeroGradient))
            .padding(20.dp)
    ) {
        Column {
            heroTimeLabel(hourly.announceTime)?.let {
                Text(
                    "지금 · $it",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.92f)
                )
            }
            Text(
                "$currentTemp°",
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                color = Color.White
            )
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                today?.maxTemp?.let { HeroMeta("최고", "$it°") }
                today?.minTemp?.let { HeroMeta("최저", "$it°") }
                today?.let { HeroMeta("오늘 강수", "${maxOf(it.amPop, it.pmPop)}%") }
            }
        }
    }
}

@Composable
private fun HeroMeta(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

// ==================== Hourly ====================

@Composable
private fun HourlyForecastCard(hourly: HourlyForecastDto) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("24시간 예보", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                updateLabel(hourly.announceTime),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            HourlyTempChart(points = hourly.hourlyPoints, announceTime = hourly.announceTime)
            Spacer(Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                itemsIndexed(hourly.hourlyPoints) { _, p ->
                    HourlyCell(
                        label = formatValidAtLabel(hourly.announceTime, p.effectiveTime),
                        temp = p.temp,
                        pop = p.pop
                    )
                }
            }
        }
    }
}

@Composable
private fun HourlyCell(label: String, temp: Int, pop: Int) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(vertical = 11.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$temp°", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("$pop%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = popColor(pop))
    }
}

// ==================== Weekly ====================

@Composable
private fun WeeklyForecastCard(daily: DailyForecastDto) {
    val points = daily.dailyPoints
    val weekMin = points.minOf { it.minTemp }
    val weekMax = points.maxOf { it.maxTemp }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("7일 예보", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "강수확률 오전 / 오후",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            points.forEachIndexed { index, d ->
                WeeklyRow(
                    label = dayLabel(d.daysAhead, daily.announceTime),
                    amPop = d.amPop,
                    pmPop = d.pmPop,
                    minTemp = d.minTemp,
                    maxTemp = d.maxTemp,
                    weekMin = weekMin,
                    weekMax = weekMax
                )
                if (index != points.lastIndex) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun WeeklyRow(
    label: String,
    amPop: Int,
    pmPop: Int,
    minTemp: Int,
    maxTemp: Int,
    weekMin: Int,
    weekMax: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.width(28.dp), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

        Row(modifier = Modifier.width(78.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("$amPop", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = popColor(amPop))
            Text("/", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$pmPop%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = popColor(pmPop))
        }

        Text(
            "$minTemp°",
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TempRangeBar(
            minTemp = minTemp,
            maxTemp = maxTemp,
            weekMin = weekMin,
            weekMax = weekMax,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )
        Text("$maxTemp°", modifier = Modifier.width(30.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TempRangeBar(minTemp: Int, maxTemp: Int, weekMin: Int, weekMax: Int, modifier: Modifier = Modifier) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val span = (weekMax - weekMin).coerceAtLeast(1)
    val startFraction = ((minTemp - weekMin).toFloat() / span).coerceIn(0f, 1f)
    val endFraction = ((maxTemp - weekMin).toFloat() / span).coerceIn(0f, 1f)

    Canvas(modifier = modifier.height(7.dp)) {
        val barHeight = size.height
        val radius = barHeight / 2f
        drawRoundRect(color = trackColor, cornerRadius = CornerRadius(radius, radius))

        val left = size.width * startFraction
        val right = (size.width * endFraction).coerceAtLeast(left + barHeight)
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(TempLow, TempHigh), startX = left, endX = right),
            topLeft = Offset(left, 0f),
            size = Size(right - left, barHeight),
            cornerRadius = CornerRadius(radius, radius)
        )
    }
}

@Composable
private fun popColor(pop: Int): Color =
    if (pop >= RAIN_POP_THRESHOLD) RainEmphasis else MaterialTheme.colorScheme.onSurfaceVariant

// ==================== Air quality ====================

@Composable
private fun AirQualitySection(airQuality: AirQualityDto?) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("대기질", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
private fun PollutantBlock(label: String, pollutant: Pollutant, value: Int?, grade: AirQualityGrade?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (value == null || grade == null) {
            Text("정보 없음", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(text = airQualityText(grade, value), fontWeight = FontWeight.Bold)
            AirQualityBar(grade = grade, fraction = pollutant.fillFraction(value))
        }
    }
}

@Composable
private fun airQualityText(grade: AirQualityGrade, value: Int) = buildAnnotatedString {
    val gradeStyle = MaterialTheme.typography.titleMedium
    val unitStyle = MaterialTheme.typography.titleSmall

    withStyle(SpanStyle(fontSize = gradeStyle.fontSize, fontWeight = FontWeight.Bold)) {
        append(grade.label)
    }
    withStyle(SpanStyle(fontSize = unitStyle.fontSize, fontWeight = FontWeight.Bold)) {
        append("(${value}㎍/㎥)")
    }
}

@Composable
private fun AirQualityBar(grade: AirQualityGrade, fraction: Float) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(10.dp)
                .clip(shape)
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

// ==================== Placeholder ====================

@Composable
private fun SectionPlaceholder(title: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("정보 없음", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ==================== Time / date helpers ====================

private val FORECAST_ZONE: ZoneId = ZoneId.of("Asia/Seoul")

private fun heroTimeLabel(announceTime: String): String? {
    val t = parseTimeToSeoul(announceTime) ?: return null
    return String.format(Locale.KOREA, "%02d:%02d 기준", t.hour, t.minute)
}

private fun updateLabel(announceTime: String): String {
    val t = parseTimeToSeoul(announceTime) ?: return ""
    return String.format(Locale.KOREA, "%02d:%02d 업데이트", t.hour, t.minute)
}

private fun baseDateOf(announceTime: String): LocalDate {
    return try {
        val at = LocalDateTime.parse(announceTime)
        if (at.hour < 6) at.toLocalDate().minusDays(1) else at.toLocalDate()
    } catch (_: Exception) {
        val zdt = parseTimeToSeoul(announceTime)
        when {
            zdt == null  -> LocalDate.now(FORECAST_ZONE)
            zdt.hour < 6 -> zdt.toLocalDate().minusDays(1)
            else         -> zdt.toLocalDate()
        }
    }
}

private fun pointDate(daysAhead: Int, announceTime: String): LocalDate =
    baseDateOf(announceTime).plusDays(daysAhead.toLong())

private fun weekdayKo(date: LocalDate): String = when (date.dayOfWeek) {
    DayOfWeek.MONDAY    -> "월"
    DayOfWeek.TUESDAY   -> "화"
    DayOfWeek.WEDNESDAY -> "수"
    DayOfWeek.THURSDAY  -> "목"
    DayOfWeek.FRIDAY    -> "금"
    DayOfWeek.SATURDAY  -> "토"
    DayOfWeek.SUNDAY    -> "일"
}

private fun dayLabel(daysAhead: Int, announceTime: String): String {
    val date = pointDate(daysAhead, announceTime)
    return if (date == LocalDate.now(FORECAST_ZONE)) "오늘" else weekdayKo(date)
}