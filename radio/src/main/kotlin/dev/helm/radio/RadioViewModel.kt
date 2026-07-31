package dev.helm.radio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.helm.sdk.McuServiceLocator
import dev.helm.sdk.RadioBand
import dev.helm.sdk.RadioStation
import dev.helm.sdk.SeekDirection
import dev.helm.sdk.StubRadioTuner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RadioViewModel(app: Application) : AndroidViewModel(app) {

    private val tuner = StubRadioTuner() // Track B: replace with TwUtilRadioTuner post-FEL
    private val presetStore = RadioPresetStore(app)
    private val isStubMode = !McuServiceLocator.isInitialized

    private val _state = MutableStateFlow(RadioState(isStubMode = isStubMode))
    val state: StateFlow<RadioState> = _state.asStateFlow()

    val presets: StateFlow<List<RadioStation?>> = presetStore.presets
        .stateIn(viewModelScope, SharingStarted.Eagerly, List(12) { null })

    init {
        viewModelScope.launch {
            tuner.currentStation().collect { station ->
                if (station != null) _state.update { it.copy(station = station) }
            }
        }
    }

    fun tune(frequencyMhz: Float, band: RadioBand) {
        viewModelScope.launch {
            _state.update { it.copy(isSeeking = false) }
            tuner.tune(frequencyMhz, band)
        }
    }

    fun seek(direction: SeekDirection) {
        viewModelScope.launch {
            _state.update { it.copy(isSeeking = true) }
            tuner.seek(direction)
            _state.update { it.copy(isSeeking = false) }
        }
    }

    fun savePreset(slot: Int) {
        viewModelScope.launch {
            presetStore.save(slot, _state.value.station)
        }
    }

    fun recallPreset(station: RadioStation) {
        tune(station.frequencyMhz, station.band)
    }
}
