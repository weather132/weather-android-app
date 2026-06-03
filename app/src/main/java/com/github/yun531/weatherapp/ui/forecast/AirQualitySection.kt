package com.github.yun531.weatherapp.ui.forecast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.github.yun531.weatherapp.data.remote.dto.AirQualityDto
import com.github.yun531.weatherapp.domain.AirQualityGrade
import com.github.yun531.weatherapp.domain.Pollutant

@Composable
internal fun AirQualitySection(airQuality: AirQualityDto?) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            SectionHeader(AirIcon, "대기질")
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

    withStyle(SpanStyle(fontSize = gradeStyle.fontSize, fontWeight = FontWeight.Bold, color = gradeColor(grade))) {
        append(grade.label)
    }
    withStyle(SpanStyle(fontSize = unitStyle.fontSize, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)) {
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