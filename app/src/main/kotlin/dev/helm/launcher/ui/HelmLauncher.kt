package dev.helm.launcher.ui

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import dev.helm.launcher.R
import androidx.compose.ui.text.style.TextAlign
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
import dev.helm.launcher.media.NowPlayingState
import dev.helm.launcher.media.NowPlayingViewModel
import dev.helm.launcher.speed.SpeedViewModel
import dev.helm.launcher.theme.ThemeViewModel
import dev.helm.launcher.weather.WeatherViewModel
import dev.helm.radio.RadioScreen
import dev.helm.sdk.CarSystem
import dev.helm.sdk.WeatherDataSource
import dev.helm.themes.ThemeDefaults
import dev.helm.widgets.WeatherWidget
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

// ─── Home ────────────────────────────────────────────────────────────────────

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
    val onAppClick: (AppEntry) -> Unit = { entry ->
        vm.onAppLaunched(entry.action)
        val target = actionToScreen(entry.action)
        if (target != null) onNavigate(target) else handleLaunch(context, entry)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        HomeTopBar(
            speedKmh = speed,
            weatherSource = weatherSource,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        )

        // Car illustration
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.car_home),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }

        HomeBottomBar(
            hotseat = vm.hotseat,
            media = media,
            nowPlayingVm = nowPlayingVm,
            onOpenNowPlaying = onOpenNowPlaying,
            onAppClick = onAppClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun HomeTopBar(
    speedKmh: Int,
    weatherSource: WeatherDataSource,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: status indicator
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            )
            Text(
                text = "Helm",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            )
        }

        // Center: speed — dominant element
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "$speedKmh",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "km/h",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
        }

        // Right: weather card + clock card
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Box(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(Spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                WeatherWidget(source = weatherSource, compact = false)
            }
            HomeClockCard()
        }
    }
}

@Composable
private fun HomeClockCard(modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            now = LocalDateTime.now()
        }
    }
    val dateFmt = remember { DateTimeFormatter.ofPattern("EEE d MMM", Locale("es")) }
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Box(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                RoundedCornerShape(12.dp),
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = dateFmt.format(now).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            )
            Text(
                text = timeFmt.format(now),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun HomeBottomBar(
    hotseat: List<AppEntry>,
    media: NowPlayingState,
    nowPlayingVm: NowPlayingViewModel,
    onOpenNowPlaying: () -> Unit,
    onAppClick: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val leftApps = hotseat.take(3)
    val rightApps = hotseat.drop(3)

    Row(
        modifier = modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leftApps.forEach { entry ->
                BottomAppIcon(entry = entry, onClick = { onAppClick(entry) })
            }
        }

        MiniPlayerCard(
            media = media,
            nowPlayingVm = nowPlayingVm,
            onExpand = onOpenNowPlaying,
            modifier = Modifier.weight(1.8f),
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            rightApps.forEach { entry ->
                BottomAppIcon(entry = entry, onClick = { onAppClick(entry) })
            }
        }
    }
}

@Composable
private fun MiniPlayerCard(
    media: NowPlayingState,
    nowPlayingVm: NowPlayingViewModel,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var positionMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(media.isPlaying) {
        while (media.isPlaying) {
            positionMs = nowPlayingVm.livePositionMs()
            delay(1_000L)
        }
    }
    val progress = if (media.durationMs > 0L) {
        (positionMs.toFloat() / media.durationMs).coerceIn(0f, 1f)
    } else 0f

    Column(
        modifier = modifier
            .neumorphicClickable(onClick = onExpand, cornerRadius = 16.dp, elevation = 5.dp)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Artwork
            val artwork = media.artwork
            if (artwork != null) {
                Image(
                    painter = BitmapPainter(artwork.asImageBitmap()),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(Modifier.width(Spacing.sm))

            Column(Modifier.weight(1f)) {
                Text(
                    text = if (media.title.isNotEmpty()) media.title else "Sin reproducción",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (media.artist.isNotEmpty()) {
                    Text(
                        text = media.artist,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Transport controls
            IconButton(onClick = { nowPlayingVm.skipPrev() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = null, modifier = Modifier.size(20.dp))
            }
            IconButton(
                onClick = { if (media.isPlaying) nowPlayingVm.pause() else nowPlayingVm.play() },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = if (media.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = { nowPlayingVm.skipNext() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.SkipNext, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }

        // Progress bar
        if (media.durationMs > 0L) {
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = formatMs(positionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(1f).height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                )
                Text(
                    text = formatMs(media.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val sec = ms / 1000L
    return "%d:%02d".format(sec / 60, sec % 60)
}

@Composable
private fun BottomAppIcon(entry: AppEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val iconScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .neumorphicClickable(
                onClick = {
                    onClick()
                    scope.launch {
                        iconScale.animateTo(0.78f, spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh))
                        iconScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy))
                    }
                },
                cornerRadius = 14.dp,
                elevation = 4.dp,
            )
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
    ) {
        AppIconImage(
            pkg = entry.pkg,
            label = entry.label,
            action = entry.action,
            modifier = Modifier.size(40.dp).scale(iconScale.value),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = entry.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

// ─── Routing helpers ─────────────────────────────────────────────────────────

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
