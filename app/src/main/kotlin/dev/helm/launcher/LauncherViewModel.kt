package dev.helm.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    val hotseat: List<AppEntry> = listOf(
        AppEntry("Navigation", "com.google.android.apps.maps", LauncherAction.NAVIGATION),
        AppEntry("Bluetooth", "com.tw.bt", LauncherAction.BLUETOOTH),
        AppEntry("Settings", "settings", LauncherAction.SETTINGS),
        AppEntry("Radio", "com.tw.radio", LauncherAction.RADIO),
        AppEntry("CarPlay", "com.zjinnova.zlink", LauncherAction.CARPLAY),
        AppEntry("Camera", "com.tw.reverse", LauncherAction.REVERSE_CAMERA),
    )

    val grid: List<AppEntry> = listOf(
        AppEntry("Music", "com.tw.music", LauncherAction.MUSIC),
        AppEntry("Video", "com.tw.video", LauncherAction.VIDEO),
        AppEntry("DVR", "com.tw.dvr", LauncherAction.DVR),
        AppEntry("EQ", "com.tw.eq", LauncherAction.EQ),
        AppEntry("AUX", "com.tw.auxin", LauncherAction.AUX),
        AppEntry("Right Cam", "com.tw.rightview", LauncherAction.RIGHT_CAMERA),
        AppEntry("360 Cam", "cn.cardoor.zt360", LauncherAction.CAMERA_360),
        AppEntry("Car Info", "com.dofun.carsetting", LauncherAction.CAR_SETTINGS),
    )

    private val _pendingMediaNav = MutableStateFlow(false)
    val pendingMediaNav: StateFlow<Boolean> = _pendingMediaNav.asStateFlow()

    fun onAppLaunched(action: LauncherAction) {
        if (action in MEDIA_ACTIONS) {
            _pendingMediaNav.value = true
            viewModelScope.launch {
                delay(3_000)
                _pendingMediaNav.value = false
            }
        }
    }

    fun consumeMediaNav() {
        _pendingMediaNav.value = false
    }

    companion object {
        private val MEDIA_ACTIONS = setOf(
            LauncherAction.RADIO,
            LauncherAction.BLUETOOTH,
            LauncherAction.CARPLAY,
            LauncherAction.MUSIC,
            LauncherAction.VIDEO,
            LauncherAction.AUX,
        )
    }
}
