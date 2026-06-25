package com.github.yun531.weatherapp.ui.briefing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.github.yun531.weatherapp.domain.briefing.RainBriefing
import java.time.LocalDate
import java.time.LocalDateTime

// ==================== 압축 요약 ====================

internal fun rainSummary(rain: RainBriefing?, today: LocalDate): String {
    if (rain == null || !rain.hasAnyRain()) return "없음"
    val firstInterval = rain.intervals.firstOrNull()
    if (firstInterval != null) return intervalStartLabel(firstInterval.start, today)
    return sevenDayRainSummary(rain.days)
}

private fun intervalStartLabel(start: LocalDateTime, today: LocalDate): String {
    if (start.toLocalDate() == today) return "${start.hour}시 시작"
    return "내일 ${start.hour}시 시작"
}

private fun sevenDayRainSummary(days: List<RainBriefing.DayRain>): String {
    val rainyDays = days.count { it.rainAm || it.rainPm }
    val nearestLabel = relativeDayLabel(firstRainyDayIndex(days))
    if (rainyDays == 1) return "$nearestLabel 비"
    return "$nearestLabel 등 ${rainyDays}일"
}

private fun firstRainyDayIndex(days: List<RainBriefing.DayRain>): Int =
    days.indexOfFirst { it.rainAm || it.rainPm }

private fun relativeDayLabel(daysAhead: Int): String = when (daysAhead) {
    0 -> "오늘"
    1 -> "내일"
    2 -> "모레"
    else -> "D+$daysAhead"
}

// ==================== 상세 ====================

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RainDetail(rain: RainBriefing, today: LocalDate) {
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        days.take(7).forEachIndexed { idx, day ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(relativeDayLabel(idx), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

// ==================== 포맷 ====================

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