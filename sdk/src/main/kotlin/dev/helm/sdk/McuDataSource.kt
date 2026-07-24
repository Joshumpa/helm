package dev.helm.sdk

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

// Implemented once we identify which system APK delivers MCU data (speed, ADAS)
interface McuDataSource {
    val speed: StateFlow<Int>
    val adasEvents: SharedFlow<AdasEvent>
}
