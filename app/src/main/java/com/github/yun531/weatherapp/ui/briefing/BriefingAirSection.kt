package com.github.yun531.weatherapp.ui.briefing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.yun531.weatherapp.domain.AirQualityGrade
import com.github.yun531.weatherapp.domain.briefing.AirBriefing

// ==================== 압축 요약 ====================

internal fun airSummary(air: AirBriefing?): String {
    if (air == null || !air.hasAnyMeasurement()) return "정보 없음"
    val grade = air.representativeGrade() ?: return "정보 없음"
    return gradeLabel(grade)
}

/** 미세먼지 압축 값 색: 막대그래프와 동일한 4단계 grade 색 */
@Composable
internal fun airValueColor(air: AirBriefing?): Color {
    val grade = air?.representativeGrade()?.toAirQualityGrade()
        ?: return MaterialTheme.colorScheme.onSurface
    return gradeColor(grade)
}

// ==================== 상세 ====================

@Composable
internal fun AirDetail(air: AirBriefing) {
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

private fun gradeLabel(grade: String): String = when (grade) {
    AirBriefing.Grade.GOOD -> "좋음"
    AirBriefing.Grade.MODERATE -> "보통"
    AirBriefing.Grade.BAD -> "나쁨"
    AirBriefing.Grade.VERY_BAD -> "매우나쁨"
    else -> grade
}