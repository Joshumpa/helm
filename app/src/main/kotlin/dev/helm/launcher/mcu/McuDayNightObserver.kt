package dev.helm.launcher.mcu

import android.content.Context
import dev.helm.sdk.DayNight
import dev.helm.sdk.McuServiceLocator
import dev.helm.themes.ThemeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

// Task 4.1 — observes McuService.dayNight and writes helm_day_night to DataStore so
// ThemeViewModel.isDark reacts without user interaction.
object McuDayNightObserver {

    fun start(context: Context, scope: CoroutineScope) {
        if (!McuServiceLocator.isInitialized) return
        val repo = ThemeRepository(context)
        McuServiceLocator.service.dayNight
            .map { it == DayNight.NIGHT }
            .onEach { isDark -> repo.setDark(isDark) }
            .launchIn(scope)
    }
}
