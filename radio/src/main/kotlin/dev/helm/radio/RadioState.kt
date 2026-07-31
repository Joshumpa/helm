package dev.helm.radio

import dev.helm.sdk.RadioBand
import dev.helm.sdk.RadioStation

data class RadioState(
    val station: RadioStation = RadioStation(frequencyMhz = 98.5f, band = RadioBand.FM),
    val isStubMode: Boolean = true,
    val isSeeking: Boolean = false,
)
