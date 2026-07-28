package dev.helm.launcher.ui

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.helm.bluetooth.BluetoothScreen
import dev.helm.carplay.CarPlayScreen
import dev.helm.core.Spacing
import dev.helm.core.neumorphicClickable
import dev.helm.core.theme.HelmTheme
import dev.helm.launcher.AppEntry
import dev.helm.launcher.LauncherAction
import dev.helm.launcher.LauncherViewModel
import dev.helm.launcher.media.NowPlayingViewModel
import dev.helm.launcher.speed.SpeedViewModel
import dev.helm.launcher.theme.ThemeViewModel
import dev.helm.launcher.weather.WeatherViewModel
import dev.helm.radio.RadioScreen
import dev.helm.sdk.CarSystem
import dev.helm.sdk.WeatherDataSource
import dev.helm.themes.ThemeDefaults
import dev.helm.widgets.WeatherWidget
import kotlinx.coroutines.delay

private enum class Screen { Splash, Home, NowPlaying, Settings, MusicLibrary, Bluetooth, Radio, CarPlay }

@Composable
fun HelmLauncher(
    vm: LauncherViewModel = viewModel(),
    nowPlayingVm: NowPlayingViewModel = viewModel(),
    themeVm: ThemeViewModel = viewModel(),
    weatherVm: WeatherViewModel = viewModel(),
    speedVm: SpeedViewModel = viewModel(),
) {
    val variant by themeVm.variant.collectAsState()
    val colorScheme = ThemeDefaults.schemeFor(variant, isSystemInDarkTheme())
    var screen by remember { mutableStateOf(Screen.Splash) }

    LaunchedEffect(Unit) {
        delay(1_600L)
        screen = Screen.Home
    }

    val media by nowPlayingVm.media.collectAsState()
    val pendingMediaNav by vm.pendingMediaNav.collectAsState()
    LaunchedEffect(media.title, pendingMediaNav) {
        if (media.title.isNotEmpty() && pendingMediaNav && screen == Screen.Home) {
            vm.consumeMediaNav()
            screen = Screen.NowPlaying
        }
    }

    HelmTheme(colorScheme = colorScheme) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = { screenTransition(initialState, targetState) },
            label = "screen",
        ) { current ->
            when (current) {
                Screen.Splash -> SplashScreen()
                Screen.NowPlaying -> NowPlayingScreen(
                    onBack = { screen = Screen.Home },
                    vm = nowPlayingVm,
                )
                Screen.Settings -> SettingsScreen(
                    themeVm = themeVm,
                    onBack = { screen = Screen.Home },
                )
                Screen.MusicLibrary -> MusicLibraryScreen(
                    onBack = { screen = Screen.Home },
                    onOpenNowPlaying = { screen = Screen.NowPlaying },
                )
                Screen.Bluetooth -> BluetoothScreen(
                    onBack = { screen = Screen.Home },
                )
                Screen.Radio -> RadioScreen(
                    onBack = { screen = Screen.Home },
                )
                Screen.CarPlay -> CarPlayScreen(
                    onBack = { screen = Screen.Home },
                )
                Screen.Home -> HomeScreen(
                    vm = vm,
                    nowPlayingVm = nowPlayingVm,
                    weatherSource = weatherVm.source,
                    speedVm = speedVm,
                    context = LocalContext.current,
                    onOpenNowPlaying = { screen = Screen.NowPlaying },
                    onNavigate = { screen = it },
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    vm: LauncherViewModel,
    nowPlayingVm: NowPlayingViewModel,
    weatherSource: WeatherDataSource,
    speedVm: SpeedViewModel,
    context: Context,
    onOpenNowPlaying: () -> Unit,
    onNavigate: (Screen) -> Unit,
) {
    val media by nowPlayingVm.media.collectAsState()
    val speed by speedVm.speedKmh.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ClockBar(modifier = Modifier.weight(1f))
            AnimatedVisibility(visible = speed > 0, enter = fadeIn(), exit = fadeOut()) {
                SpeedBadge(speedKmh = speed, modifier = Modifier.padding(end = Spacing.sm))
            }
            WeatherWidget(source = weatherSource, compact = true)
            Spacer(Modifier.width(Spacing.sm))
            IconButton(onClick = { onNavigate(Screen.Settings) }) {
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
                val artwork = media.artwork
                if (artwork != null) {
                    Image(
                        painter = BitmapPainter(artwork.asImageBitmap()),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("♪", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
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
            onAppClick = { entry ->
                vm.onAppLaunched(entry.action)
                val target = actionToScreen(entry.action)
                if (target != null) onNavigate(target) else handleLaunch(context, entry)
            },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        Hotseat(
            apps = vm.hotseat,
            onAppClick = { entry ->
                vm.onAppLaunched(entry.action)
                val target = actionToScreen(entry.action)
                if (target != null) onNavigate(target) else handleLaunch(context, entry)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
        )
    }
}

@Composable
private fun SpeedBadge(speedKmh: Int, modifier: Modifier = Modifier) {
    val bgColor by animateColorAsState(
        targetValue = when {
            speedKmh >= 90 -> MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
            speedKmh >= 50 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            else           -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(400),
        label = "speed_bg",
    )
    val numColor by animateColorAsState(
        targetValue = when {
            speedKmh >= 90 -> MaterialTheme.colorScheme.error
            speedKmh >= 50 -> MaterialTheme.colorScheme.primary
            else           -> MaterialTheme.colorScheme.onBackground
        },
        animationSpec = tween(400),
        label = "speed_num",
    )

    Row(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(20.dp))
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$speedKmh",
            style = MaterialTheme.typography.titleMedium,
            color = numColor,
        )
        Text(
            text = " km/h",
            style = MaterialTheme.typography.labelSmall,
            color = numColor.copy(alpha = 0.65f),
        )
    }
}

private fun actionToScreen(action: LauncherAction): Screen? = when (action) {
    LauncherAction.MUSIC      -> Screen.MusicLibrary
    LauncherAction.SETTINGS   -> Screen.Settings
    LauncherAction.BLUETOOTH  -> Screen.Bluetooth
    LauncherAction.RADIO      -> Screen.Radio
    LauncherAction.CARPLAY    -> Screen.CarPlay
    else                      -> null
}

private fun screenTransition(from: Screen, to: Screen): ContentTransform {
    if (from == Screen.Splash) {
        return fadeIn(tween(600)) togetherWith fadeOut(tween(400))
    }
    if (to == Screen.NowPlaying) {
        return (slideInVertically(tween(300, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(300))) togetherWith
            (slideOutVertically(tween(240, easing = FastOutSlowInEasing)) { -it / 5 } + fadeOut(tween(200)))
    }
    if (from == Screen.NowPlaying) {
        return (slideInVertically(tween(300, easing = FastOutSlowInEasing)) { -it / 5 } + fadeIn(tween(300))) togetherWith
            (slideOutVertically(tween(240, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(200)))
    }
    return if (to != Screen.Home) {
        (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(300))) togetherWith
            (slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) { -it / 5 } + fadeOut(tween(200)))
    } else {
        (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it / 5 } + fadeIn(tween(300))) togetherWith
            (slideOutHorizontally(tween(240, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(200)))
    }
}

private fun handleLaunch(context: Context, entry: AppEntry) {
    when (entry.action) {
        LauncherAction.REVERSE_CAMERA -> CarSystem.openReverseCamera(context)
        LauncherAction.RIGHT_CAMERA   -> CarSystem.openRightCamera(context)
        LauncherAction.CAMERA_360     -> CarSystem.openCamera360(context)
        LauncherAction.VIDEO          -> CarSystem.openVideo(context)
        LauncherAction.DVR            -> CarSystem.openDvr(context)
        LauncherAction.EQ             -> CarSystem.openEq(context)
        LauncherAction.AUX            -> CarSystem.openAux(context)
        LauncherAction.CAR_SETTINGS   -> CarSystem.openCarSettings(context)
        LauncherAction.SCREENSAVER    -> CarSystem.triggerScreenSaver(context)
        else -> Unit
    }
}
