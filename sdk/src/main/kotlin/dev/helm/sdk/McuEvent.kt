package dev.helm.sdk

sealed class McuEvent {
    data class DayNightChanged(val isNight: Boolean) : McuEvent()
    data class CarPlaySessionChanged(val active: Boolean) : McuEvent()
    data class SteeringVolumeChanged(val level: Int, val maxLevel: Int, val muted: Boolean) : McuEvent()
    data class McuVolumeLevels(val mediaLevel: Int, val secondaryLevel: Int) : McuEvent()
    data class AudioSourceChanged(val sourceId: Int) : McuEvent()
    data class PhoneCallChanged(val active: Boolean) : McuEvent()
    data class ReverseGearChanged(val engaged: Boolean) : McuEvent()
    data class CameraStreamChanged(val active: Boolean) : McuEvent()
    data class CameraResolutionChanged(val width: Int, val height: Int) : McuEvent()
    data class ParkingRadarChanged(
        val front: List<Int>,
        val frontMax: Int,
        val rear: List<Int>,
        val rearMax: Int,
    ) : McuEvent()
    data class AmbientTempChanged(val celsius: Int?) : McuEvent()
    data class DoorStateChanged(val state: DoorState) : McuEvent()
    data class McuVersionReceived(val version: String) : McuEvent()
    data object Heartbeat : McuEvent()
    data class PowerOffPending(val delayMs: Int) : McuEvent()
    data class RtcTimeSyncReceived(
        val hour: Int,
        val minute: Int,
        val second: Int,
        val year: Int,
        val month: Int,
        val day: Int,
    ) : McuEvent()
    data class HardwareMuteChanged(val muted: Boolean) : McuEvent()
    data class ScreenRotationChanged(val rotation: Int) : McuEvent()
    data class Raw(val code: Int, val arg1: Int, val arg2: Int) : McuEvent()
}
