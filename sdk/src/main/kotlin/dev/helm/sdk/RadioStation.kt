package dev.helm.sdk

enum class RadioBand { FM, AM }

enum class SeekDirection { FORWARD, BACKWARD }

data class RadioStation(
    val frequencyMhz: Float,
    val band: RadioBand,
    val name: String? = null,
)
