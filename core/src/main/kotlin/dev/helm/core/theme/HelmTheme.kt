package dev.helm.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HelmColorScheme = darkColorScheme(
    background = HelmBackground,
    surface = HelmSurface,
    surfaceVariant = HelmSurfaceVariant,
    primary = HelmPrimary,
    onPrimary = HelmOnPrimary,
    onBackground = HelmOnBackground,
    onSurface = HelmOnSurface,
    error = HelmError,
    outline = HelmDivider,
    outlineVariant = HelmSurfaceVariant,
)

@Composable
fun HelmTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HelmColorScheme,
        typography = HelmTypography,
        content = content,
    )
}
