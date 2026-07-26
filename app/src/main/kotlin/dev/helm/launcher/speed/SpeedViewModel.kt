package dev.helm.launcher.speed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow

class SpeedViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = GpsSpeedRepository(app)
    val speedKmh: StateFlow<Int> = repo.speedKmh

    fun onLocationPermissionGranted() = repo.start()

    override fun onCleared() {
        repo.stop()
        super.onCleared()
    }
}
