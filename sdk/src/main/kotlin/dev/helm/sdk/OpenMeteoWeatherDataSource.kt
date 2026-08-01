package dev.helm.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class OpenMeteoWeatherDataSource(
    private val context: Context,
    scope: CoroutineScope,
) : WeatherDataSource {

    private val _state = MutableStateFlow<WeatherState?>(null)
    override val state: StateFlow<WeatherState?> = _state

    private var lastKnown: WeatherState? = null
    private var lastFetchMs: Long = 0L

    init {
        scope.launch {
            while (true) {
                _state.value = fetch()
                delay(30.minutes)
            }
        }
    }

    private suspend fun fetch(): WeatherState? {
        val location = getLocation() ?: return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val url = URL(
                    "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=${location.latitude}" +
                        "&longitude=${location.longitude}" +
                        "&current=temperature_2m,weather_code"
                )
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5_000
                conn.readTimeout = 5_000
                try {
                    val json = conn.inputStream.bufferedReader().readText()
                    val current = JSONObject(json).getJSONObject("current")
                    WeatherState(
                        condition = wmoToCondition(current.getInt("weather_code")),
                        temperatureCelsius = current.getDouble("temperature_2m").toFloat(),
                    ).also {
                        lastKnown = it
                        lastFetchMs = System.currentTimeMillis()
                    }
                } finally {
                    conn.disconnect()
                }
            }.getOrElse {
                val ageMs = System.currentTimeMillis() - lastFetchMs
                if (lastKnown != null && ageMs < 30.minutes.inWholeMilliseconds) lastKnown else null
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLocation(): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
            val last = runCatching { lm.getLastKnownLocation(provider) }.getOrNull()
            if (last != null && System.currentTimeMillis() - last.time < 10.minutes.inWholeMilliseconds) {
                return last
            }
        }

        val activeProvider = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return null
        }

        return withTimeoutOrNull(3.seconds) {
            suspendCancellableCoroutine { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(loc: Location) {
                        if (cont.isActive) cont.resume(loc)
                    }
                    override fun onProviderDisabled(provider: String) {
                        if (cont.isActive) cont.resume(null)
                    }
                }
                runCatching {
                    @Suppress("DEPRECATION")
                    lm.requestSingleUpdate(activeProvider, listener, context.mainLooper)
                }.onFailure { if (cont.isActive) cont.resume(null) }

                cont.invokeOnCancellation { runCatching { lm.removeUpdates(listener) } }
            }
        }
    }

    private fun wmoToCondition(code: Int): WeatherCondition = when (code) {
        0 -> WeatherCondition.CLEAR
        in 1..3 -> WeatherCondition.CLOUDY
        45, 48 -> WeatherCondition.HAZE
        in 51..67 -> WeatherCondition.RAIN
        in 71..77 -> WeatherCondition.SNOW
        in 80..82 -> WeatherCondition.RAIN
        85, 86 -> WeatherCondition.SNOW
        in 95..99 -> WeatherCondition.THUNDERSTORM
        else -> WeatherCondition.CLOUDY
    }
}
