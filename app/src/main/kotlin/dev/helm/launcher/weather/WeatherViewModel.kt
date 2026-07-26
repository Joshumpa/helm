package dev.helm.launcher.weather

import androidx.lifecycle.ViewModel
import dev.helm.sdk.WeatherDataSource
import dev.helm.sdk.WeatherDataSourceStub

class WeatherViewModel : ViewModel() {
    val source: WeatherDataSource = WeatherDataSourceStub()
}
