package com.github.yun531.weatherapp.ui.forecast

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yun531.weatherapp.data.remote.dto.ForecastHourlyPoint

private const val RAIN_THRESHOLD = 60

@Composable
fun HourlyTempChart(
    points: List<ForecastHourlyPoint>,
    announceTime: String,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return

    val lineColor = MaterialTheme.colorScheme.primary
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    val dotColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val rainBgColor = RainTint
    val rainAccentColor = RainEmphasis
    val density = LocalDensity.current
    val tempLabelSizePx = with(density) { 11.sp.toPx() }
    val timeLabelSizePx = with(density) { 10.sp.toPx() }
    val rainLabelSizePx = with(density) { 9.sp.toPx() }

    val temps = points.map { it.temp }
    val minTemp = temps.min()
    val maxTemp = temps.max()
    val tempRange = (maxTemp - minTemp).coerceAtLeast(1)

    val labelInterval = when {
        points.size <= 8  -> 1
        points.size <= 12 -> 2
        points.size <= 18 -> 3
        else              -> 4
    }

    val maxIdx = temps.indexOf(maxTemp)
    val minIdx = temps.indexOf(minTemp)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
    ) {
        val textArgb = textColor.toArgb()
        val subTextArgb = subTextColor.toArgb()
        val rainAccentArgb = rainAccentColor.toArgb()

        val paddingLeft = 16.dp.toPx()
        val paddingRight = 16.dp.toPx()
        val paddingTop = 28.dp.toPx()
        val paddingBottom = 32.dp.toPx()

        val chartWidth = size.width - paddingLeft - paddingRight
        val chartHeight = size.height - paddingTop - paddingBottom

        val stepX = chartWidth / (points.size - 1)

        val coordinates = points.mapIndexed { i, p ->
            val x = paddingLeft + stepX * i
            val y = paddingTop + chartHeight * (1f - (p.temp - minTemp).toFloat() / tempRange)
            Offset(x, y)
        }

        // 비 구간 배경 하이라이트
        val halfStep = stepX / 2
        points.forEachIndexed { i, p ->
            if (p.pop >= RAIN_THRESHOLD) {
                val x = coordinates[i].x
                drawRect(
                    color = rainBgColor,
                    topLeft = Offset(x - halfStep, paddingTop),
                    size = Size(stepX, chartHeight)
                )
            }
        }

        // 그라데이션 영역
        val fillPath = Path().apply {
            moveTo(coordinates.first().x, paddingTop + chartHeight)
            coordinates.forEach { lineTo(it.x, it.y) }
            lineTo(coordinates.last().x, paddingTop + chartHeight)
            close()
        }
        drawPath(
            fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor, fillColor.copy(alpha = 0f)),
                startY = paddingTop,
                endY = paddingTop + chartHeight
            )
        )

        // 라인
        val linePath = Path().apply {
            moveTo(coordinates.first().x, coordinates.first().y)
            for (i in 1 until coordinates.size) {
                lineTo(coordinates[i].x, coordinates[i].y)
            }
        }
        drawPath(linePath, lineColor, style = Stroke(width = 2.dp.toPx()))

        // Paint
        val tempPaint = android.graphics.Paint().apply {
            color = textArgb
            textSize = tempLabelSizePx
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }

        val timePaint = android.graphics.Paint().apply {
            color = subTextArgb
            textSize = timeLabelSizePx
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        val rainTimePaint = android.graphics.Paint().apply {
            color = rainAccentArgb
            textSize = timeLabelSizePx
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        val rainPaint = android.graphics.Paint().apply {
            color = rainAccentArgb
            textSize = rainLabelSizePx
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        // 점 + 기온 라벨 + 강수확률
        coordinates.forEachIndexed { i, coord ->
            val isRainy = points[i].pop >= RAIN_THRESHOLD
            val showLabel = (i % labelInterval == 0) || i == maxIdx || i == minIdx

            if (showLabel || isRainy) {
                val currentDotColor = if (isRainy) rainAccentColor else dotColor
                drawCircle(currentDotColor, radius = 3.5.dp.toPx(), center = coord)
            }

            if (showLabel) {
                drawContext.canvas.nativeCanvas.drawText(
                    "${temps[i]}°",
                    coord.x,
                    coord.y - 10.dp.toPx(),
                    tempPaint
                )
            }

            // 비 시간대: 연속 구간의 첫/마지막 또는 값 변경 시점만 표시
            if (isRainy) {
                val prevRainy = i > 0 && points[i - 1].pop >= RAIN_THRESHOLD
                val nextRainy = i < points.size - 1 && points[i + 1].pop >= RAIN_THRESHOLD
                val prevSamePop = prevRainy && points[i - 1].pop == points[i].pop
                val nextSamePop = nextRainy && points[i + 1].pop == points[i].pop

                val showPop = !prevSamePop || !nextSamePop
                val label = if (showPop) "${points[i].pop}%" else "·"

                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    coord.x,
                    coord.y + 14.dp.toPx(),
                    rainPaint
                )
            }
        }

        // 시간 라벨 (하단)
        coordinates.forEachIndexed { i, coord ->
            if (i % labelInterval == 0) {
                val label = formatChartTimeLabel(announceTime, points[i].effectiveTime)
                val currentPaint = if (points[i].pop >= RAIN_THRESHOLD) rainTimePaint else timePaint

                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    coord.x,
                    size.height - 6.dp.toPx(),
                    currentPaint
                )
            }
        }
    }
}

private fun formatChartTimeLabel(announceTime: String, effectiveTime: String): String {
    val t = parseTimeToSeoul(effectiveTime) ?: return ""
    return String.format("%02d시", t.hour)
}