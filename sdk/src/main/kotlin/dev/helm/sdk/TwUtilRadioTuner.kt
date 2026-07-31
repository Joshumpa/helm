package dev.helm.sdk

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// Track B only — radio commands via MCU UART. Exact command codes TBD post-FEL
// decompile of com.tw.radio. MCU owns QN8035 SPI bus; no direct JNI needed.
@Suppress("UnusedPrivateMember")
internal class TwUtilRadioTuner(private val service: McuService) : RadioTuner {

    private val _current = MutableStateFlow<RadioStation?>(null)

    override fun currentStation(): Flow<RadioStation?> = _current.asStateFlow()

    override suspend fun tune(frequencyMhz: Float, band: RadioBand): Result<Unit> = runCatching {
        // TODO post-FEL: derive exact MCU tune command from com.tw.radio decompile.
        // Expected pattern: service.send(TUNE_CMD, encodeFreq(frequencyMhz, band), bandId)
        _current.value = RadioStation(frequencyMhz, band)
    }

    override suspend fun seek(direction: SeekDirection): Result<Unit> = runCatching {
        // TODO post-FEL: derive seek command from com.tw.radio decompile.
        // Expected pattern: service.send(SEEK_CMD, if (direction == SeekDirection.FORWARD) 1 else 0)
    }
}
