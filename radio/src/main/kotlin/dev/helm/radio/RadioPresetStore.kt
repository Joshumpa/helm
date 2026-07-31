package dev.helm.radio

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.helm.sdk.RadioBand
import dev.helm.sdk.RadioStation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.radioDataStore: DataStore<Preferences> by preferencesDataStore(name = "helm_radio")

private const val MAX_PRESETS = 12

class RadioPresetStore(context: Context) {

    private val dataStore = context.applicationContext.radioDataStore

    val presets: Flow<List<RadioStation?>> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            (0 until MAX_PRESETS).map { i ->
                prefs[stringPreferencesKey("preset_$i")]?.let { decode(it) }
            }
        }

    suspend fun save(slot: Int, station: RadioStation) {
        require(slot in 0 until MAX_PRESETS)
        dataStore.edit { it[stringPreferencesKey("preset_$slot")] = encode(station) }
    }

    suspend fun clear(slot: Int) {
        require(slot in 0 until MAX_PRESETS)
        dataStore.edit { it.remove(stringPreferencesKey("preset_$slot")) }
    }

    private fun encode(s: RadioStation): String = "${s.band.name}:${s.frequencyMhz}:${s.name.orEmpty()}"

    private fun decode(raw: String): RadioStation? = runCatching {
        val parts = raw.split(":")
        RadioStation(
            band = RadioBand.valueOf(parts[0]),
            frequencyMhz = parts[1].toFloat(),
            name = parts.getOrNull(2)?.takeIf { it.isNotEmpty() },
        )
    }.getOrNull()
}
