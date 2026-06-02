package com.github.yun531.weatherapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val WeatherLightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = OnBrand,
    primaryContainer = BrandContainer,
    onPrimaryContainer = OnBrandContainer,
    inversePrimary = BrandInverse,

    secondary = AccentSteel,
    onSecondary = OnBrand,
    secondaryContainer = BrandContainer,
    onSecondaryContainer = OnBrandContainer,

    tertiary = AccentSlate,
    onTertiary = OnBrand,
    tertiaryContainer = SlateContainer,
    onTertiaryContainer = TextPrimary,

    background = PageBackground,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = TextSecondary,
    inverseSurface = InverseSurface,
    inverseOnSurface = PageBackground,

    surfaceBright = SurfaceBright,
    surfaceDim = SurfaceDim,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,

    outline = OutlineStrong,
    outlineVariant = OutlineSoft,

    error = WarningRed,
    onError = OnBrand,
    errorContainer = WarningContainer,
    onErrorContainer = OnWarningContainer
)

@Composable
fun WeatherAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WeatherLightColors,
        content = content
    )
}