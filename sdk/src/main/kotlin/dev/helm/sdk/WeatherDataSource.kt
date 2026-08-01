package dev.helm.sdk

import kotlinx.coroutines.flow.StateFlow

data class WeatherState(
    val condition: WeatherCondition = WeatherCondition.CLEAR,
    val temperatureCelsius: Float = 22f,
)

interface WeatherDataSource {
    val state: StateFlow<WeatherState?>
}
