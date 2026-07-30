package dev.helm.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.helm.core.Spacing
import dev.helm.core.neumorphicClickable
import dev.helm.core.neumorphicShadow
import dev.helm.themes.HelmThemeVariant
import dev.helm.themes.ThemeDefaults

private enum class Section { Root, Appearance, Display, Sound, About }

@Composable
fun SettingsScreen(
    currentTheme: HelmThemeVariant,
    onThemeChange: (HelmThemeVariant) -> Unit,
    onBack: () -> Unit,
    vm: SettingsViewModel = viewModel(),
) {
    var section by remember { mutableStateOf(Section.Root) }

    AnimatedContent(
        targetState = section,
        transitionSpec = { sectionTransition(targetState) },
        label = "settings_section",
    ) { current ->
        when (current) {
            Section.Root -> SettingsRoot(
                onSection = { section = it },
                onBack = onBack,
            )
            Section.Appearance -> AppearanceSection(
                currentTheme = currentTheme,
                onThemeChange = onThemeChange,
                onBack = { section = Section.Root },
            )
            Section.Display -> DisplaySection(
                vm = vm,
                onBack = { section = Section.Root },
            )
            Section.Sound -> SoundSection(
                vm = vm,
                onBack = { section = Section.Root },
            )
            Section.About -> AboutSection(
                onBack = { section = Section.Root },
            )
        }
    }
}

private fun sectionTransition(to: Section): ContentTransform =
    if (to != Section.Root) {
        (slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(280))) togetherWith
            (slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) { -it / 5 } + fadeOut(tween(200)))
    } else {
        (slideInHorizontally(tween(280, easing = FastOutSlowInEasing)) { -it / 5 } + fadeIn(tween(280))) togetherWith
            (slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(200)))
    }

// ─── Root ─────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsRoot(
    onSection: (Section) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        ScreenHeader(title = "Ajustes", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SettingsRow(
                icon = Icons.Filled.Palette,
                title = "Apariencia",
                subtitle = "Tema del sistema",
                onClick = { onSection(Section.Appearance) },
            )
            SettingsRow(
                icon = Icons.Filled.BrightnessHigh,
                title = "Pantalla",
                subtitle = "Brillo de la pantalla",
                onClick = { onSection(Section.Display) },
            )
            SettingsRow(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = "Sonido",
                subtitle = "Volumen de medios",
                onClick = { onSection(Section.Sound) },
            )
            SettingsRow(
                icon = Icons.Filled.Info,
                title = "Acerca de",
                subtitle = "Versión y dispositivo",
                onClick = { onSection(Section.About) },
            )
        }
    }
}

// ─── Appearance ───────────────────────────────────────────────────────────────

@Composable
private fun AppearanceSection(
    currentTheme: HelmThemeVariant,
    onThemeChange: (HelmThemeVariant) -> Unit,
    onBack: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        ScreenHeader(title = "Apariencia", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            HelmThemeVariant.entries.chunked(2).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    row.forEach { variant ->
                        ThemeCard(
                            variant = variant,
                            isDark = isDark,
                            isSelected = variant == currentTheme,
                            onClick = { onThemeChange(variant) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size < 2) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ThemeCard(
    variant: HelmThemeVariant,
    isDark: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = ThemeDefaults.schemeFor(variant, isDark)

    Box(
        modifier = modifier
            .aspectRatio(1.6f)
            .neumorphicClickable(
                onClick = onClick,
                cornerRadius = 20.dp,
                elevation = 6.dp,
                showAccentBorder = isSelected,
            )
            .padding(Spacing.md),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ColorDot(scheme.background)
                ColorDot(scheme.primary)
                ColorDot(scheme.surface)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = variant.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorDot(color: Color) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(color),
    )
}

// ─── Display ──────────────────────────────────────────────────────────────────

@Composable
private fun DisplaySection(
    vm: SettingsViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val brightness by vm.brightness.collectAsState()
    val canWrite = remember { vm.canWriteSettings() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        ScreenHeader(title = "Pantalla", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SettingsSectionCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.BrightnessHigh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "Brillo",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${(brightness * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (canWrite) {
                    Slider(
                        value = brightness,
                        onValueChange = { vm.setBrightness(it) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                } else {
                    Text(
                        text = "Se necesita permiso para modificar el brillo del sistema",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = Spacing.sm)
                            .neumorphicClickable(
                                onClick = {
                                    context.startActivity(
                                        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                                            .setData(Uri.parse("package:${context.packageName}")),
                                    )
                                },
                                cornerRadius = 12.dp,
                                elevation = 4.dp,
                            )
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    ) {
                        Text(
                            text = "Conceder permiso",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

// ─── Sound ────────────────────────────────────────────────────────────────────

@Composable
private fun SoundSection(
    vm: SettingsViewModel,
    onBack: () -> Unit,
) {
    val mediaVolume by vm.mediaVolume.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        ScreenHeader(title = "Sonido", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SettingsSectionCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        text = "Medios",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "${(mediaVolume * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Slider(
                    value = mediaVolume,
                    onValueChange = { vm.setMediaVolume(it) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }
        }
    }
}

// ─── About ────────────────────────────────────────────────────────────────────

@Composable
private fun AboutSection(onBack: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "—"
        }.getOrDefault("—")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        ScreenHeader(title = "Acerca de", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            SettingsSectionCard {
                AboutRow(label = "Versión de Helm", value = versionName)
                AboutRow(label = "Android", value = android.os.Build.VERSION.RELEASE)
                AboutRow(label = "Dispositivo", value = android.os.Build.MODEL)
                AboutRow(label = "API mínima", value = "29 (Android 10)")
            }
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

// ─── Shared components ────────────────────────────────────────────────────────

@Composable
private fun ScreenHeader(
    title: String,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .neumorphicClickable(onClick = onClick, cornerRadius = 14.dp, elevation = 4.dp)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun SettingsSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val bg = MaterialTheme.colorScheme.background
    Column(
        modifier = modifier
            .fillMaxWidth()
            .neumorphicShadow(backgroundColor = bg, cornerRadius = 16.dp, elevation = 6.dp)
            .background(bg, RoundedCornerShape(16.dp))
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        content = content,
    )
}
