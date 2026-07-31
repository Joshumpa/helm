package dev.helm.sdk

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class McuService internal constructor(
    private val source: McuDataSource,
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val events = source.events()
        .shareIn(scope, SharingStarted.Eagerly, replay = 0)

    val dayNight: StateFlow<DayNight> = events
        .filterIsInstance<McuEvent.DayNightChanged>()
        .map { if (it.isNight) DayNight.NIGHT else DayNight.DAY }
        .stateIn(scope, SharingStarted.Eagerly, DayNight.DAY)

    val audioSource: StateFlow<Int> = events
        .filterIsInstance<McuEvent.AudioSourceChanged>()
        .map { it.sourceId }
        .stateIn(scope, SharingStarted.Eagerly, AudioSourceId.IDLE)

    val reverseGear: StateFlow<Boolean> = events
        .filterIsInstance<McuEvent.ReverseGearChanged>()
        .map { it.engaged }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val carPlayActive: StateFlow<Boolean> = events
        .filterIsInstance<McuEvent.CarPlaySessionChanged>()
        .map { it.active }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val doorState: StateFlow<DoorState> = events
        .filterIsInstance<McuEvent.DoorStateChanged>()
        .map { it.state }
        .stateIn(scope, SharingStarted.Eagerly, DoorState())

    val ambientTemperature: StateFlow<Int?> = events
        .filterIsInstance<McuEvent.AmbientTempChanged>()
        .map { it.celsius }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val cameraStreamActive: StateFlow<Boolean> = events
        .filterIsInstance<McuEvent.CameraStreamChanged>()
        .map { it.active }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val cameraResolution: StateFlow<Pair<Int, Int>?> = events
        .filterIsInstance<McuEvent.CameraResolutionChanged>()
        .map { Pair(it.width, it.height) }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val parkingRadar: SharedFlow<McuEvent.ParkingRadarChanged> = events
        .filterIsInstance<McuEvent.ParkingRadarChanged>()
        .shareIn(scope, SharingStarted.Eagerly, replay = 1)

    val steeringVolume: SharedFlow<McuEvent.SteeringVolumeChanged> = events
        .filterIsInstance<McuEvent.SteeringVolumeChanged>()
        .shareIn(scope, SharingStarted.Eagerly, replay = 0)

    val mcuVolumeLevels: SharedFlow<McuEvent.McuVolumeLevels> = events
        .filterIsInstance<McuEvent.McuVolumeLevels>()
        .shareIn(scope, SharingStarted.Eagerly, replay = 0)

    val phoneCallActive: StateFlow<Boolean> = events
        .filterIsInstance<McuEvent.PhoneCallChanged>()
        .map { it.active }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val hardwareMuted: StateFlow<Boolean> = events
        .filterIsInstance<McuEvent.HardwareMuteChanged>()
        .map { it.muted }
        .stateIn(scope, SharingStarted.Eagerly, false)

    val powerOffCountdown: SharedFlow<McuEvent.PowerOffPending> = events
        .filterIsInstance<McuEvent.PowerOffPending>()
        .shareIn(scope, SharingStarted.Eagerly, replay = 0)

    val rtcTimeSync: SharedFlow<McuEvent.RtcTimeSyncReceived> = events
        .filterIsInstance<McuEvent.RtcTimeSyncReceived>()
        .shareIn(scope, SharingStarted.Eagerly, replay = 0)

    // Speed: privileged path codes unknown until post-FEL; stub emits 0.
    private val _speed = MutableStateFlow(0)
    val speed: StateFlow<Int> = _speed.asStateFlow()

    // Task 1.9 — power-off: ack MCU, schedule shutdown. Requires system privileges (Track B).
    init {
        scope.launch {
            powerOffCountdown.collect { event ->
                send(0x9F10, 0, 3000) // ack with 3000 ms buffer
                runCatching {
                    @Suppress("DEPRECATION")
                    context.sendBroadcast(
                        Intent("android.intent.action.ACTION_REQUEST_SHUTDOWN").apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                    )
                }
            }
        }

        // Task 1.10 — RTC time sync: set system clock from MCU on boot.
        scope.launch {
            rtcTimeSync.collect { sync -> applyRtcTime(sync) }
        }

        // Task 7.2 — CarPlay audio source: set to CARPLAY on session start, restore on end.
        scope.launch {
            var prevSource = AudioSourceId.IDLE
            carPlayActive.collect { active ->
                if (active) {
                    prevSource = audioSource.value
                    send(0x0301, 192, AudioSourceId.CARPLAY)
                } else if (prevSource != AudioSourceId.IDLE) {
                    send(0x0301, 192, prevSource)
                    prevSource = AudioSourceId.IDLE
                }
            }
        }

        // Task 1.10 — push time back to MCU RTC on TIME_TICK / TIME_SET.
        scope.launch {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val now = java.util.Calendar.getInstance()
                    val payload = byteArrayOf(
                        (now.get(java.util.Calendar.HOUR_OF_DAY)).toByte(),
                        (now.get(java.util.Calendar.MINUTE)).toByte(),
                        (now.get(java.util.Calendar.SECOND)).toByte(),
                        (now.get(java.util.Calendar.YEAR) - 2000).toByte(),
                        (now.get(java.util.Calendar.MONTH) + 1).toByte(),
                        (now.get(java.util.Calendar.DAY_OF_MONTH)).toByte(),
                        0,
                    )
                    scope.launch { send(0x0107, 0, 0, payload) }
                }
            }
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
            }
            context.registerReceiver(receiver, filter)
        }
    }

    // SET_TIME is a system-only permission granted post-FEL; runCatching handles the pre-root case.
    @android.annotation.SuppressLint("MissingPermission")
    private fun applyRtcTime(sync: McuEvent.RtcTimeSyncReceived) {
        runCatching {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val cal = java.util.Calendar.getInstance().apply {
                set(sync.year, sync.month - 1, sync.day, sync.hour, sync.minute, sync.second)
            }
            @Suppress("DEPRECATION")
            alarmManager.setTime(cal.timeInMillis)
        }
    }

    suspend fun send(code: Int, arg1: Int = 0, arg2: Int = 0, data: ByteArray = ByteArray(0)): Result<Unit> =
        source.send(code, arg1, arg2, data)
}
