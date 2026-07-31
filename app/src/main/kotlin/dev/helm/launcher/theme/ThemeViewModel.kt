package dev.helm.launcher.theme

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.helm.themes.HelmThemeVariant
import dev.helm.themes.ThemeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ThemeViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = ThemeRepository(app)

    val variant: StateFlow<HelmThemeVariant> = repository.selectedVariant
        .stateIn(viewModelScope, SharingStarted.Eagerly, HelmThemeVariant.HELM)

    // Task 4.2 — driven by MCU 0x0204 via McuDayNightObserver; falls back to false (day).
    val isDark: StateFlow<Boolean> = repository.isDark
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setVariant(variant: HelmThemeVariant) {
        viewModelScope.launch { repository.setVariant(variant) }
    }
}
