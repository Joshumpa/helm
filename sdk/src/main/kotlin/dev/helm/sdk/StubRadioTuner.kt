package dev.helm.sdk

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class StubRadioTuner : RadioTuner {
    override fun currentStation(): Flow<RadioStation?> =
        flowOf(RadioStation(frequencyMhz = 98.5f, band = RadioBand.FM, name = null))

    override suspend fun tune(frequencyMhz: Float, band: RadioBand): Result<Unit> =
        Result.success(Unit)

    override suspend fun seek(direction: SeekDirection): Result<Unit> =
        Result.success(Unit)
}
