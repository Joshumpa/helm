package dev.helm.core.theme

import androidx.compose.ui.graphics.Color

// ── Helm dark ─────────────────────────────────────────────────────────────────
// Deep navy base — neumorphism requires a non-black background so shadows can
// appear both lighter (top-left) and darker (bottom-right) than the surface.

val HelmBackground    = Color(0xFF1A1E2E)
val HelmSurface       = Color(0xFF1D2234)
val HelmSurfaceVariant = Color(0xFF232940)
val HelmPrimary       = Color(0xFF4F9EFF)
val HelmOnPrimary     = Color(0xFFFFFFFF)
val HelmOnBackground  = Color(0xFFE8EBF5)
val HelmOnSurface     = Color(0xFFCDD3EF)
val HelmSubtext       = Color(0xFF8E93AA)
val HelmDivider       = Color(0xFF2E3450)
val HelmSuccess       = Color(0xFF30D158)
val HelmWarning       = Color(0xFFFF9F0A)
val HelmError         = Color(0xFFFF453A)

// ── Placeholder colors for app icons without a real icon ──────────────────────
val PlaceholderColors = listOf(
    Color(0xFF1565C0),
    Color(0xFF2E7D32),
    Color(0xFF6A1B9A),
    Color(0xFFE65100),
    Color(0xFF00838F),
    Color(0xFFC62828),
    Color(0xFF37474F),
    Color(0xFFF57F17),
)
