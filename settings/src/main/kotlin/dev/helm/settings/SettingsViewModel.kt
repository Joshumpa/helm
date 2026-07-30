package dev.helm.settings

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val audio = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val resolver = application.contentResolver

    private val _brightness = MutableStateFlow(readBrightness())
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private val _mediaVolume = MutableStateFlow(readMediaVolume())
    val mediaVolume: StateFlow<Float> = _mediaVolume.asStateFlow()

    fun canWriteSettings(): Boolean = Settings.System.canWrite(getApplication())

    fun setBrightness(value: Float) {
        if (!canWriteSettings()) return
        val intVal = (value * 255).toInt().coerceIn(1, 255)
        Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, intVal)
        _brightness.value = value
    }

    fun setMediaVolume(value: Float) {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (value * max).toInt(), 0)
        _mediaVolume.value = value
    }

    private fun readBrightness(): Float =
        Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f

    private fun readMediaVolume(): Float {
        val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return if (max > 0) current.toFloat() / max else 0.5f
    }
}
