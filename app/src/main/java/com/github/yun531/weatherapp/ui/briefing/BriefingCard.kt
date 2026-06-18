package com.github.yun531.weatherapp.ui.briefing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.yun531.weatherapp.data.region.RegionCatalog
import com.github.yun531.weatherapp.domain.AirQualityGrade
import com.github.yun531.weatherapp.domain.briefing.AirBriefing
import com.github.yun531.weatherapp.domain.briefing.RainBriefing
import com.github.yun531.weatherapp.domain.briefing.RegionBriefing
import com.github.yun531.weatherapp.domain.briefing.WarningBriefing
import java.time.LocalDate
import java.time.LocalDateTime

// ==================== 의미 색 팔레트 ====================
// 영역별 의미 색. 카드 좌측 막대 / 압축 행 값 강조 등에 사용.
// 미세먼지 값/막대는 4단계 gradeColor 를 따로 사용한다.

private val RainAccent = Color(0xFF1976D2)       // 파랑 - 비
private val WarningAccent = Color(0xFFC62828)    // 빨강 - 기상특보
private val AirAccent = Color(0xFFE65100)        // 진한 주황 - 미세먼지 emphasis 좌측 막대

@Composable
fun BriefingCard(
    briefing: RegionBriefing,
    initiallyExpanded: Boolean,
    today: LocalDate,
    regionCatalog: RegionCatalog,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable(briefing.regionId) { mutableStateOf(initiallyExpanded) }
    val emphasis = emphasisOf(briefing)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            AccentBar(emphasis)
            Column(modifier = Modifier.padding(14.dp).fillMaxWidth()) {
                HeaderRow(
                    regionName = regionCatalog.nameOf(briefing.regionId),
                    badgeText = badgeTextOf(emphasis, briefing),
                    emphasis = emphasis,
                    expanded = expanded
                )
                Spacer(Modifier.height(10.dp))
                CompactRow(briefing)

                AnimatedVisibility(visible = expanded) {
                    Column {
                        if (briefing.rain != null && briefing.rain.days.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            RainDetail(briefing.rain, today)
                        }
                        if (briefing.air != null && briefing.air.hasAnyMeasurement()) {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(12.dp))
                            AirDetail(briefing.air)
                        }
                    }
                }
            }
        }
    }
}

// ==================== Emphasis (좌측 막대 + 뱃지) ====================

private enum class CardEmphasis { WARNING, AIR_BAD, RAIN, CLEAR }

private fun emphasisOf(b: RegionBriefing): CardEmphasis = when {
    b.warnings.isNotEmpty() -> CardEmphasis.WARNING
    b.air?.representativeGrade()
        ?.let { it == AirBriefing.Grade.BAD || it == AirBriefing.Grade.VERY_BAD } == true -> CardEmphasis.AIR_BAD
    b.rain?.hasAnyRain() == true -> CardEmphasis.RAIN
    else -> CardEmphasis.CLEAR
}

private fun badgeTextOf(emphasis: CardEmphasis, b: RegionBriefing): String = when (emphasis) {
    CardEmphasis.WARNING -> "기상특보 ${b.warnings.size}건"
    CardEmphasis.AIR_BAD -> "미세먼지 주의"
    CardEmphasis.RAIN -> "비 예정"
    CardEmphasis.CLEAR -> "특이사항 없음"
}

@Composable
private fun AccentBar(emphasis: CardEmphasis) {
    Box(
        modifier = Modifier
            .width(4.dp)
            .fillMaxHeight()
            .background(accentColor(emphasis))
    )
}

@Composable
private fun accentColor(emphasis: CardEmphasis): Color = when (emphasis) {
    CardEmphasis.WARNING -> WarningAccent
    CardEmphasis.AIR_BAD -> AirAccent
    CardEmphasis.RAIN    -> RainAccent
    CardEmphasis.CLEAR   -> MaterialTheme.colorScheme.outlineVariant
}

@Composable
private fun badgeColors(emphasis: CardEmphasis): Pair<Color, Color> = when (emphasis) {
    CardEmphasis.WARNING -> MaterialTheme.colorScheme.errorContainer to
            MaterialTheme.colorScheme.onErrorContainer
    CardEmphasis.AIR_BAD -> Color(0xFFFFE0B2) to Color(0xFF7A4F00)
    CardEmphasis.RAIN    -> Color(0xFFDCEBFB) to Color(0xFF0C447C)
    CardEmphasis.CLEAR   -> MaterialTheme.colorScheme.surfaceVariant to
            MaterialTheme.colorScheme.onSurfaceVariant
}

// ==================== Header ====================

@Composable
private fun HeaderRow(
    regionName: String,
    badgeText: String,
    emphasis: CardEmphasis,
    expanded: Boolean
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = regionName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        StatusBadge(badgeText, emphasis)
        Spacer(Modifier.width(6.dp))
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "접기" else "펼치기",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusBadge(text: String, emphasis: CardEmphasis) {
    val (bg, fg) = badgeColors(emphasis)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = fg)
    }
}

// ==================== Compact row ====================

@Composable
private fun CompactRow(b: RegionBriefing) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CompactCell(
            label = "비",
            value = rainSummary(b.rain),
            valueColor = if (b.rain?.hasAnyRain() == true) RainAccent else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        CompactCell(
            label = "기상특보",
            value = warningSummary(b.warnings),
            valueColor = if (b.warnings.isNotEmpty()) WarningAccent else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        CompactCell(
            label = "(초)미세먼지",
            value = airSummary(b.air),
            valueColor = airValueColor(b.air),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CompactCell(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}

/** 미세먼지 압축 값 색: 막대그래프와 동일한 4단계 grade 색 */
@Composable
private fun airValueColor(air: AirBriefing?): Color {
    val grade = air?.representativeGrade()?.toAirQualityGrade()
        ?: return MaterialTheme.colorScheme.onSurface
    return gradeColor(grade)
}

private fun rainSummary(rain: RainBriefing?): String {
    if (rain == null || !rain.hasAnyRain()) return "없음"
    val firstInterval = rain.intervals.firstOrNull()
    if (firstInterval != null) return "${firstInterval.start.hour}시 시작"
    val rainyDays = rain.days.count { it.rainAm || it.rainPm }
    return "7일 중 ${rainyDays}일"
}

private fun warningSummary(warnings: List<WarningBriefing>): String =
    if (warnings.isEmpty()) "없음"
    else warnings.joinToString(", ") { kindLabel(it.kind) }

private fun airSummary(air: AirBriefing?): String {
    if (air == null || !air.hasAnyMeasurement()) return "정보 없음"
    val grade = air.representativeGrade() ?: return "정보 없음"
    return gradeLabel(grade)
}

// ==================== Rain detail ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RainDetail(rain: RainBriefing, today: LocalDate) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (rain.intervals.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                maxItemsInEachRow = 2,
                modifier = Modifier.fillMaxWidth()
            ) {
                rain.intervals.forEach { iv ->
                    AssistChip(
                        onClick = {},
                        label = { Text(formatInterval(iv.start, iv.end, today)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }
        }
        // 7일 막대는 days 가 있으면 무조건 표시 (비 없어도 회색 막대)
        Text(
            "7일 오전/오후 비 여부",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SevenDayRainBars(rain.days)
    }
}

@Composable
private fun SevenDayRainBars(days: List<RainBriefing.DayRain>) {
    val labels = listOf("오늘", "내일", "모레", "D+3", "D+4", "D+5", "D+6")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        days.take(7).forEachIndexed { idx, day ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(labels.getOrElse(idx) { "D+$idx" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                RainBar(active = day.rainAm)
                Spacer(Modifier.height(2.dp))
                RainBar(active = day.rainPm)
            }
        }
    }
}

@Composable
private fun RainBar(active: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(
                if (active) RainAccent
                else MaterialTheme.colorScheme.surfaceVariant
            )
    )
}

// ==================== Air detail ====================

@Composable
private fun AirDetail(air: AirBriefing) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PmColumn(
            label = "미세먼지",
            value = air.pm10,
            gradeStr = air.pm10Grade,
            withValue = air.pm10NeedsValue(),
            modifier = Modifier.weight(1f)
        )
        PmColumn(
            label = "초미세먼지",
            value = air.pm25,
            gradeStr = air.pm25Grade,
            withValue = air.pm25NeedsValue(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PmColumn(
    label: String,
    value: Int?,
    gradeStr: String?,
    withValue: Boolean,
    modifier: Modifier = Modifier
) {
    val gradeEnum = gradeStr?.toAirQualityGrade()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (gradeEnum == null || value == null) {
            Text("정보 없음", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(
                text = if (withValue) "${gradeEnum.label} ${value}㎍/㎥" else gradeEnum.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = gradeColor(gradeEnum)
            )
            AirQualityMiniBar(gradeEnum, fraction = fillFractionOf(label, value))
        }
    }
}

@Composable
private fun AirQualityMiniBar(grade: AirQualityGrade, fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(gradeColor(grade))
        )
    }
}

private fun fillFractionOf(label: String, value: Int): Float {
    val max = if (label == "미세먼지") 150f else 75f
    return (value / max).coerceIn(0.05f, 1f)
}

// ==================== Formatting ====================

private fun formatInterval(start: LocalDateTime, end: LocalDateTime, today: LocalDate): String =
    "${formatTime(start, today)}~${formatTime(end, today)}"

private fun formatTime(dt: LocalDateTime, today: LocalDate): String {
    val day = when (dt.toLocalDate()) {
        today -> "오늘"
        today.plusDays(1) -> "내일"
        today.plusDays(2) -> "모레"
        else -> "${dt.monthValue}/${dt.dayOfMonth}"
    }
    return "$day ${dt.hour}시"
}

private fun gradeLabel(grade: String): String = when (grade) {
    AirBriefing.Grade.GOOD -> "좋음"
    AirBriefing.Grade.MODERATE -> "보통"
    AirBriefing.Grade.BAD -> "나쁨"
    AirBriefing.Grade.VERY_BAD -> "매우나쁨"
    else -> grade
}

private fun kindLabel(kind: String): String = when (kind) {
    "RAIN" -> "호우"
    "HEAT" -> "폭염"
    "WIND" -> "강풍"
    "COLD" -> "한파"
    "SNOW" -> "대설"
    "DRY" -> "건조"
    else -> kind
}