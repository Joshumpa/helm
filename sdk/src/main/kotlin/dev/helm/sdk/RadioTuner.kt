package dev.helm.sdk

import kotlinx.coroutines.flow.Flow

interface RadioTuner {
    fun currentStation(): Flow<RadioStation?>
    suspend fun tune(frequencyMhz: Float, band: RadioBand): Result<Unit>
    suspend fun seek(direction: SeekDirection): Result<Unit>
}
