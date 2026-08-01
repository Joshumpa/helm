package dev.helm.launcher.weather

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.helm.sdk.OpenMeteoWeatherDataSource
import dev.helm.sdk.WeatherDataSource

class WeatherViewModel(app: Application) : AndroidViewModel(app) {
    val source: WeatherDataSource = OpenMeteoWeatherDataSource(app, viewModelScope)
}
