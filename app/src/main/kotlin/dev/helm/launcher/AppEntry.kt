package dev.helm.launcher

enum class LauncherAction {
    RADIO, BLUETOOTH, CARPLAY, NAVIGATION,
    REVERSE_CAMERA, RIGHT_CAMERA, CAMERA_360,
    MUSIC, VIDEO, DVR, EQ, AUX,
    CAR_SETTINGS, SETTINGS, SCREENSAVER,
}

data class AppEntry(
    val label: String,
    val pkg: String,
    val action: LauncherAction,
)
