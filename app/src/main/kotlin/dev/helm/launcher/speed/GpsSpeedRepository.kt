package dev.helm.launcher.speed

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.roundToInt

class GpsSpeedRepository(context: Context) {

    private val _speedKmh = MutableStateFlow(0)
    val speedKmh: StateFlow<Int> = _speedKmh.asStateFlow()

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val appContext = context.applicationContext

    private val listener = LocationListener { location ->
        _speedKmh.value = if (location.hasSpeed() && location.speed > 0f)
            (location.speed * 3.6f).roundToInt()
        else
            0
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (!hasPermission()) return
        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1_000L,
                0f,
                listener,
                Looper.getMainLooper(),
            )
        } catch (_: Exception) {
            // GPS unavailable on this device config — speed stays 0
        }
    }

    fun stop() {
        locationManager.removeUpdates(listener)
        _speedKmh.value = 0
    }

    private fun hasPermission() =
        appContext.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}
