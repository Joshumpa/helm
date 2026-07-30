package dev.helm.ota

sealed interface OtaState {
    data object Idle : OtaState
    data object Checking : OtaState
    data object UpToDate : OtaState
    data class UpdateAvailable(val info: OtaInfo) : OtaState
    data class Downloading(val progress: Float) : OtaState
    data class ReadyToInstall(val apkPath: String) : OtaState
    data class Error(val message: String) : OtaState
}
