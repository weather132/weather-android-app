package com.github.yun531.weatherapp.ui.forecast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

private fun strokeIcon(pathBuilder: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = pathBuilder
            )
        }
        .build()

private fun fillIcon(pathBuilder: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply { path(fill = SolidColor(Color.Black), pathBuilder = pathBuilder) }
        .build()

internal val RainDropIcon: ImageVector = fillIcon {
    moveTo(12f, 2.5f)
    curveTo(12f, 2.5f, 5f, 11f, 5f, 15.5f)
    arcToRelative(7f, 7f, 0f, false, false, 14f, 0f)
    curveTo(19f, 11f, 12f, 2.5f, 12f, 2.5f)
    close()
}

internal val ClockIcon: ImageVector = strokeIcon {
    moveTo(3f, 12f)
    arcToRelative(9f, 9f, 0f, true, true, 18f, 0f)
    arcToRelative(9f, 9f, 0f, true, true, -18f, 0f)
    moveTo(12f, 7f)
    verticalLineToRelative(5f)
    lineToRelative(3f, 2f)
}

internal val CalendarIcon: ImageVector = strokeIcon {
    moveTo(5f, 5f)
    lineTo(19f, 5f)
    arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
    lineTo(21f, 19f)
    arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
    lineTo(5f, 21f)
    arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
    lineTo(3f, 7f)
    arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
    close()
    moveTo(3f, 9f)
    lineTo(21f, 9f)
    moveTo(8f, 3f)
    verticalLineToRelative(4f)
    moveTo(16f, 3f)
    verticalLineToRelative(4f)
}

internal val AirIcon: ImageVector = strokeIcon {
    moveTo(3f, 8f)
    horizontalLineToRelative(11f)
    arcToRelative(3f, 3f, 0f, true, false, -3f, -3f)
    moveTo(3f, 12f)
    horizontalLineToRelative(15f)
    arcToRelative(2.5f, 2.5f, 0f, true, true, -2.5f, 2.5f)
    moveTo(3f, 16f)
    horizontalLineToRelative(9f)
    arcToRelative(2.5f, 2.5f, 0f, true, true, -2.5f, 2.5f)
}