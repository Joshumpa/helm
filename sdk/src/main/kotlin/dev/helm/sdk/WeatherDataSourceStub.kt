package dev.helm.sdk

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WeatherDataSourceStub(
    condition: WeatherCondition = WeatherCondition.CLEAR,
    temp: Float = 22f,
) : WeatherDataSource {
    override val state: StateFlow<WeatherState?> = MutableStateFlow(WeatherState(condition, temp))
}
