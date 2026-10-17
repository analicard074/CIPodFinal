package com.example.cipod_final.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private val CoastalColorScheme = lightColorScheme(
    primary = ChannelBlue,
    onPrimary = WarmCream,
    primaryContainer = Seafoam,
    onPrimaryContainer = ChannelBlue,
    secondary = OceanTeal,
    onSecondary = CoastalInk,
    secondaryContainer = Seafoam,
    onSecondaryContainer = ChannelBlue,
    tertiary = SunsetOrange,
    onTertiary = CoastalInk,
    tertiaryContainer = SunsetOrange,
    onTertiaryContainer = CoastalInk,
    background = WarmCream,
    onBackground = ChannelBlue,
    surface = WarmCream,
    onSurface = ChannelBlue,
    surfaceVariant = Seafoam,
    onSurfaceVariant = CoastalMuted,
    surfaceDim = Seafoam,
    surfaceBright = WarmCream,
    surfaceContainerLowest = WarmCream,
    surfaceContainerLow = WarmCream,
    surfaceContainer = Seafoam,
    surfaceContainerHigh = Seafoam,
    surfaceContainerHighest = Seafoam,
    outline = CoastalOutline,
    outlineVariant = CoastalOutline,
    inverseSurface = ChannelBlue,
    inverseOnSurface = WarmCream,
    inversePrimary = Seafoam,
    error = CoastalError,
    onError = WarmCream,
    errorContainer = CoastalErrorContainer,
    onErrorContainer = CoastalError
)

// Phase 1 uses the light coastal palette on both platforms.
@Composable
fun CIPodTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CoastalColorScheme,
        typography = CIPodTypography,
        shapes = CIPodShapes
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            content = content
        )
    }
}
