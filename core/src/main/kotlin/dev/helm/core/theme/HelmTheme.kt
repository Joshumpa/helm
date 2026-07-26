package dev.helm.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

val HelmColorScheme: ColorScheme = darkColorScheme(
    background     = HelmBackground,
    surface        = HelmSurface,
    surfaceVariant = HelmSurfaceVariant,
    onSurfaceVariant = HelmSubtext,
    primary        = HelmPrimary,
    onPrimary      = HelmOnPrimary,
    onBackground   = HelmOnBackground,
    onSurface      = HelmOnSurface,
    error          = HelmError,
    outline        = HelmDivider,
    outlineVariant = HelmSurfaceVariant,
)

@Composable
fun HelmTheme(
    colorScheme: ColorScheme = HelmColorScheme,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = HelmTypography,
        shapes      = HelmShapes,
        content     = content,
    )
}
