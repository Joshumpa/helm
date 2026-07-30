package dev.helm.sdk

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

// Implemented once we identify which system APK delivers MCU data (speed, ADAS)
interface McuDataSource {
    val speed: StateFlow<Int>
    val adasEvents: SharedFlow<AdasEvent>
}

// Template for callbackFlow implementations of this interface (Track B):
//
//   val speed: StateFlow<Int> = callbackFlow {
//       val cb = object : IUartReceiver.Stub() {
//           override fun onUartDataUpdate(bytes: ByteArray) { trySend(parseSpeed(bytes)) }
//       }
//       uart.registerCallback(cb)
//       awaitClose { uart.unregisterCallback(cb) }   // ← required to prevent callback leak
//   }.stateIn(scope, SharingStarted.Eagerly, 0)
