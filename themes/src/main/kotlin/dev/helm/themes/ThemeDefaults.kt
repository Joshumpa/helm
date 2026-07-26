package dev.helm.themes

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ── Helm (default) ───────────────────────────────────────────────────────────
// Pure black base, cobalt-blue accent — the original Helm design language.

private val HelmDark = darkColorScheme(
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFF8E8E93),
    primary = Color(0xFF0A84FF),
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFEBEBF5),
    error = Color(0xFFFF453A),
    outline = Color(0xFF38383A),
    outlineVariant = Color(0xFF2C2C2E),
)

private val HelmLight = lightColorScheme(
    background = Color(0xFFF2F2F7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF6C6C70),
    primary = Color(0xFF007AFF),
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    onSurface = Color(0xFF1C1C1E),
    error = Color(0xFFFF3B30),
    outline = Color(0xFFC6C6C8),
    outlineVariant = Color(0xFFE5E5EA),
)

// ── Tesla ────────────────────────────────────────────────────────────────────
// Near-black base, Tesla red accent, minimal chrome.

private val TeslaDark = darkColorScheme(
    background = Color(0xFF0A0A0A),
    surface = Color(0xFF161616),
    surfaceVariant = Color(0xFF222222),
    onSurfaceVariant = Color(0xFF909090),
    primary = Color(0xFFE31937),
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFE8E8E8),
    error = Color(0xFFFF453A),
    outline = Color(0xFF2E2E2E),
    outlineVariant = Color(0xFF1C1C1C),
)

private val TeslaLight = lightColorScheme(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF7F7F7),
    surfaceVariant = Color(0xFFEFEFEF),
    onSurfaceVariant = Color(0xFF6B6B6B),
    primary = Color(0xFFCC0000),
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0A0A0A),
    onSurface = Color(0xFF161616),
    error = Color(0xFFCC0000),
    outline = Color(0xFFD0D0D0),
    outlineVariant = Color(0xFFE8E8E8),
)

// ── Android Auto ─────────────────────────────────────────────────────────────
// Material You dark base, Google blue accent.

private val AndroidAutoDark = darkColorScheme(
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFF292929),
    onSurfaceVariant = Color(0xFF9E9E9E),
    primary = Color(0xFF4285F4),
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFFE3E3E3),
    onSurface = Color(0xFFE3E3E3),
    error = Color(0xFFF28B82),
    outline = Color(0xFF3C3C3C),
    outlineVariant = Color(0xFF292929),
)

private val AndroidAutoLight = lightColorScheme(
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F3F4),
    onSurfaceVariant = Color(0xFF5F6368),
    primary = Color(0xFF1967D2),
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF202124),
    onSurface = Color(0xFF202124),
    error = Color(0xFFC5221F),
    outline = Color(0xFFDADCE0),
    outlineVariant = Color(0xFFE8EAED),
)

// ── CarPlay ──────────────────────────────────────────────────────────────────
// Dark-gray base (warmer than Helm), sky-blue accent — Apple CarPlay feel.

private val CarPlayDark = darkColorScheme(
    background = Color(0xFF1C1C1E),
    surface = Color(0xFF2C2C2E),
    surfaceVariant = Color(0xFF3A3A3C),
    onSurfaceVariant = Color(0xFF98989D),
    primary = Color(0xFF5AC8FA),
    onPrimary = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFEBEBF5),
    error = Color(0xFFFF453A),
    outline = Color(0xFF48484A),
    outlineVariant = Color(0xFF3A3A3C),
)

private val CarPlayLight = lightColorScheme(
    background = Color(0xFFF2F2F7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF6C6C70),
    primary = Color(0xFF007AFF),
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    onSurface = Color(0xFF1C1C1E),
    error = Color(0xFFFF3B30),
    outline = Color(0xFFC6C6C8),
    outlineVariant = Color(0xFFE5E5EA),
)

// ─────────────────────────────────────────────────────────────────────────────

object ThemeDefaults {

    val all: Map<HelmThemeVariant, HelmThemeColors> = mapOf(
        HelmThemeVariant.HELM to HelmThemeColors(HelmDark, HelmLight),
        HelmThemeVariant.TESLA to HelmThemeColors(TeslaDark, TeslaLight),
        HelmThemeVariant.ANDROID_AUTO to HelmThemeColors(AndroidAutoDark, AndroidAutoLight),
        HelmThemeVariant.CARPLAY to HelmThemeColors(CarPlayDark, CarPlayLight),
    )

    fun schemeFor(variant: HelmThemeVariant, isDark: Boolean): ColorScheme {
        val colors = all[variant] ?: all[HelmThemeVariant.HELM]!!
        return if (isDark) colors.dark else colors.light
    }
}
