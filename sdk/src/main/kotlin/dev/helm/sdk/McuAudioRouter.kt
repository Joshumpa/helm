package dev.helm.sdk

import android.content.Context
import android.media.AudioManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

// Synchronises Android AudioManager state with MCU steering-wheel controls and source switches.
// All operations require system privileges for the hardware mute path; others work on Track A.
class McuAudioRouter(
    context: Context,
    private val service: McuService,
    private val scope: CoroutineScope,
) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var preMutedVolume = -1
    private var preCallSource = AudioSourceId.IDLE

    fun start() {
        // Task 2.2 — steering-wheel volume
        service.steeringVolume.onEach { ev ->
            if (ev.muted) {
                preMutedVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
            } else {
                if (preMutedVolume >= 0) {
                    audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
                    preMutedVolume = -1
                }
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, ev.level, 0)
            }
        }.launchIn(scope)

        // Task 2.3 — MCU-pushed volume levels (code 0x0106)
        service.mcuVolumeLevels.onEach { ev ->
            if (ev.mediaLevel >= 0) {
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, ev.mediaLevel, 0)
            }
        }.launchIn(scope)

        // Task 2.5 — BT phone call routing
        service.phoneCallActive.onEach { active ->
            if (active) {
                preCallSource = service.audioSource.value
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
            } else {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
                if (preCallSource != AudioSourceId.IDLE) {
                    service.send(0x0301, 192, preCallSource)
                    preCallSource = AudioSourceId.IDLE
                }
            }
        }.launchIn(scope)

        // Task 2.6 — hardware mute signal (code 0x9F1B, stream 3 = STREAM_MUSIC)
        service.hardwareMuted.onEach { muted ->
            if (muted) {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0)
            } else {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
            }
        }.launchIn(scope)
    }
}
