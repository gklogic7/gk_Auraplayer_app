package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AuraColorScheme = darkColorScheme(
    primary = AlabasterWhite,
    onPrimary = UmberBackdropDeep,
    secondary = AmberWaveform,
    onSecondary = UmberBackdropDeep,
    tertiary = CoralPinAccent,
    background = UmberBackdropDeep,
    onBackground = AlabasterWhite,
    surface = SmokedGlassSurface,
    onSurface = AlabasterWhite,
    surfaceVariant = SmokedGlassElevated,
    onSurfaceVariant = WarmSandMuted,
    outline = GlassBorderSpecular
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AuraColorScheme,
        typography = Typography,
        content = content
    )
}
