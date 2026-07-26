package dev.helm.launcher.ui

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.helm.core.Spacing
import dev.helm.core.neumorphicClickable
import dev.helm.core.theme.HelmTheme
import dev.helm.launcher.AppEntry
import dev.helm.launcher.LauncherAction
import dev.helm.launcher.LauncherViewModel
import dev.helm.launcher.media.NowPlayingViewModel
import dev.helm.launcher.theme.ThemeViewModel
import dev.helm.sdk.CarSystem
import dev.helm.themes.ThemeDefaults

private enum class Screen { Home, NowPlaying, Settings }

@Composable
fun HelmLauncher(
    vm: LauncherViewModel = viewModel(),
    nowPlayingVm: NowPlayingViewModel = viewModel(),
    themeVm: ThemeViewModel = viewModel(),
) {
    val variant by themeVm.variant.collectAsState()
    val colorScheme = ThemeDefaults.schemeFor(variant, isSystemInDarkTheme())
    var screen by remember { mutableStateOf(Screen.Home) }

    HelmTheme(colorScheme = colorScheme) {
        AnimatedContent(targetState = screen, label = "screen") { current ->
            when (current) {
                Screen.NowPlaying -> NowPlayingScreen(
                    onBack = { screen = Screen.Home },
                    vm = nowPlayingVm,
                )
                Screen.Settings -> SettingsScreen(
                    themeVm = themeVm,
                    onBack = { screen = Screen.Home },
                )
                Screen.Home -> HomeScreen(
                    vm = vm,
                    nowPlayingVm = nowPlayingVm,
                    context = LocalContext.current,
                    onOpenNowPlaying = { screen = Screen.NowPlaying },
                    onOpenSettings = { screen = Screen.Settings },
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    vm: LauncherViewModel,
    nowPlayingVm: NowPlayingViewModel,
    context: Context,
    onOpenNowPlaying: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val media by nowPlayingVm.media.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ClockBar(modifier = Modifier.weight(1f))
            Spacer(Modifier.width(Spacing.sm))
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Configuración",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        if (media.title.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs)
                    .neumorphicClickable(
                        onClick = onOpenNowPlaying,
                        cornerRadius = Spacing.md,
                        elevation = Spacing.sm,
                    )
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("♪", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(Spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = media.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = media.artist,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = if (media.isPlaying) "▶" else "⏸",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        AppGrid(
            apps = vm.grid,
            onAppClick = { handleLaunch(context, it) },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        Hotseat(
            apps = vm.hotseat,
            onAppClick = { handleLaunch(context, it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
        )
    }
}

private fun handleLaunch(context: Context, entry: AppEntry) {
    when (entry.action) {
        LauncherAction.RADIO          -> CarSystem.openRadio(context)
        LauncherAction.BLUETOOTH      -> CarSystem.openBluetooth(context)
        LauncherAction.CARPLAY        -> CarSystem.openCarPlay(context)
        LauncherAction.REVERSE_CAMERA -> CarSystem.openReverseCamera(context)
        LauncherAction.RIGHT_CAMERA   -> CarSystem.openRightCamera(context)
        LauncherAction.CAMERA_360     -> CarSystem.openCamera360(context)
        LauncherAction.MUSIC          -> CarSystem.openMusic(context)
        LauncherAction.VIDEO          -> CarSystem.openVideo(context)
        LauncherAction.DVR            -> CarSystem.openDvr(context)
        LauncherAction.EQ             -> CarSystem.openEq(context)
        LauncherAction.AUX            -> CarSystem.openAux(context)
        LauncherAction.CAR_SETTINGS   -> CarSystem.openCarSettings(context)
        LauncherAction.SETTINGS       -> CarSystem.openSettings(context)
        LauncherAction.SCREENSAVER    -> CarSystem.triggerScreenSaver(context)
    }
}
