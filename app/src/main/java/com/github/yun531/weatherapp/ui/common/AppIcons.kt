package com.github.yun531.weatherapp.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

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

internal val PinIcon: ImageVector = strokeIcon {
    moveTo(12f, 22f)
    curveTo(12f, 22f, 19f, 14f, 19f, 9f)
    arcToRelative(7f, 7f, 0f, true, false, -14f, 0f)
    curveTo(5f, 14f, 12f, 22f, 12f, 22f)
    close()
    moveTo(14.5f, 9f)
    arcToRelative(2.5f, 2.5f, 0f, true, true, -5f, 0f)
    arcToRelative(2.5f, 2.5f, 0f, true, true, 5f, 0f)
    close()
}

internal val BellIcon: ImageVector = strokeIcon {
    moveTo(18f, 8f)
    arcToRelative(6f, 6f, 0f, false, false, -12f, 0f)
    curveToRelative(0f, 7f, -3f, 9f, -3f, 9f)
    horizontalLineToRelative(18f)
    curveToRelative(0f, 0f, -3f, -2f, -3f, -9f)
    moveTo(13.7f, 21f)
    arcToRelative(2f, 2f, 0f, false, true, -3.4f, 0f)
}

internal val CloudIcon: ImageVector = strokeIcon {
    moveTo(5f, 16f)
    arcToRelative(4f, 4f, 0f, false, true, 0.5f, -8f)
    arcToRelative(5.5f, 5.5f, 0f, false, true, 10.5f, 1f)
    arcTo(3.5f, 3.5f, 0f, false, true, 16f, 16f)
    close()
}

internal val WarningIcon: ImageVector = strokeIcon {
    moveTo(12f, 3f)
    lineTo(1.5f, 21f)
    horizontalLineToRelative(21f)
    close()
    moveTo(12f, 9f)
    verticalLineToRelative(5f)
    moveTo(12f, 17.5f)
    verticalLineToRelative(0.5f)
}

internal val ThermometerIcon: ImageVector = strokeIcon {
    moveTo(10f, 13.5f)
    verticalLineTo(5f)
    arcToRelative(2f, 2f, 0f, true, true, 4f, 0f)
    verticalLineToRelative(8.5f)
    arcToRelative(4f, 4f, 0f, true, true, -4f, 0f)
    close()
}

internal val DotsIcon: ImageVector = fillIcon {
    moveTo(6.5f, 12f)
    arcToRelative(1.5f, 1.5f, 0f, true, true, -3f, 0f)
    arcToRelative(1.5f, 1.5f, 0f, true, true, 3f, 0f)
    close()
    moveTo(13.5f, 12f)
    arcToRelative(1.5f, 1.5f, 0f, true, true, -3f, 0f)
    arcToRelative(1.5f, 1.5f, 0f, true, true, 3f, 0f)
    close()
    moveTo(20.5f, 12f)
    arcToRelative(1.5f, 1.5f, 0f, true, true, -3f, 0f)
    arcToRelative(1.5f, 1.5f, 0f, true, true, 3f, 0f)
    close()
}

internal val ChevronRightIcon: ImageVector = strokeIcon {
    moveTo(9f, 6f)
    lineToRelative(6f, 6f)
    lineToRelative(-6f, 6f)
}