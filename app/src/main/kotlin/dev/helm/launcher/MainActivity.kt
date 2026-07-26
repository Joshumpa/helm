package dev.helm.launcher

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import dev.helm.launcher.speed.SpeedViewModel
import dev.helm.launcher.ui.HelmLauncher

class MainActivity : ComponentActivity() {

    private val speedVm: SpeedViewModel by viewModels()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[android.Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            speedVm.onLocationPermissionGranted()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        startGpsOrRequestPermission()
        setContent { HelmLauncher() }
    }

    private fun startGpsOrRequestPermission() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            speedVm.onLocationPermissionGranted()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                )
            )
        }
    }
}
