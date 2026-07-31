package dev.helm.carplay

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.helm.sdk.CarPlayState
import dev.helm.sdk.McuServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CarPlayViewModel(app: Application) : AndroidViewModel(app) {

    val isStubMode: Boolean = !McuServiceLocator.isInitialized

    // Task 7.1 — session state from McuService.carPlayActive (code 0x0205)
    val sessionState: StateFlow<CarPlayState> = if (McuServiceLocator.isInitialized) {
        McuServiceLocator.service.carPlayActive
            .map { active -> if (active) CarPlayState.ACTIVE else CarPlayState.DISCONNECTED }
            .stateIn(viewModelScope, SharingStarted.Eagerly, CarPlayState.DISCONNECTED)
    } else {
        kotlinx.coroutines.flow.MutableStateFlow(CarPlayState.DISCONNECTED)
    }
}
