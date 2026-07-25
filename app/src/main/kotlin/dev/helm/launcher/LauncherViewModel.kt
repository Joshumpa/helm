package dev.helm.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    val hotseat: List<AppEntry> = listOf(
        AppEntry("Radio", "com.tw.radio", LauncherAction.RADIO),
        AppEntry("Bluetooth", "com.tw.bt", LauncherAction.BLUETOOTH),
        AppEntry("CarPlay", "com.zjinnova.zlink", LauncherAction.CARPLAY),
        AppEntry("Camera", "com.tw.reverse", LauncherAction.REVERSE_CAMERA),
        AppEntry("Settings", "settings", LauncherAction.SETTINGS),
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
}
