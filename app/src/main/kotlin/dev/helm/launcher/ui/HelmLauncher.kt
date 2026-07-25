package dev.helm.launcher.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.helm.core.Spacing
import dev.helm.core.theme.HelmTheme
import dev.helm.launcher.AppEntry
import dev.helm.launcher.LauncherAction
import dev.helm.launcher.LauncherViewModel
import dev.helm.sdk.CarSystem

@Composable
fun HelmLauncher(vm: LauncherViewModel = viewModel()) {
    val context = LocalContext.current

    HelmTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            ClockBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )

            AppGrid(
                apps = vm.grid,
                onAppClick = { handleLaunch(context, it) },
                modifier = Modifier
                    .weight(1f)
                    .padding(Spacing.md),
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )

            Hotseat(
                apps = vm.hotseat,
                onAppClick = { handleLaunch(context, it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.sm),
            )
        }
    }
}

private fun handleLaunch(context: Context, entry: AppEntry) {
    when (entry.action) {
        LauncherAction.RADIO -> CarSystem.openRadio(context)
        LauncherAction.BLUETOOTH -> CarSystem.openBluetooth(context)
        LauncherAction.CARPLAY -> CarSystem.openCarPlay(context)
        LauncherAction.REVERSE_CAMERA -> CarSystem.openReverseCamera(context)
        LauncherAction.RIGHT_CAMERA -> CarSystem.openRightCamera(context)
        LauncherAction.CAMERA_360 -> CarSystem.openCamera360(context)
        LauncherAction.MUSIC -> CarSystem.openMusic(context)
        LauncherAction.VIDEO -> CarSystem.openVideo(context)
        LauncherAction.DVR -> CarSystem.openDvr(context)
        LauncherAction.EQ -> CarSystem.openEq(context)
        LauncherAction.AUX -> CarSystem.openAux(context)
        LauncherAction.CAR_SETTINGS -> CarSystem.openCarSettings(context)
        LauncherAction.SETTINGS -> CarSystem.openSettings(context)
        LauncherAction.SCREENSAVER -> CarSystem.triggerScreenSaver(context)
    }
}
