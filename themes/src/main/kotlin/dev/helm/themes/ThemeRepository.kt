package dev.helm.themes

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

// Unencrypted Preferences DataStore — stores only non-sensitive UI preferences (theme, day/night).
// Do NOT store credentials, tokens, or user data here; use EncryptedSharedPreferences instead.
private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "helm_theme")

class ThemeRepository(context: Context) {

    private val dataStore = context.applicationContext.themeDataStore
    private val variantKey = stringPreferencesKey("variant")
    private val dayNightKey = booleanPreferencesKey("helm_day_night")

    val selectedVariant: Flow<HelmThemeVariant> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            runCatching { HelmThemeVariant.valueOf(prefs[variantKey] ?: "") }
                .getOrDefault(HelmThemeVariant.HELM)
        }

    // isDark defaults to false (day); set to true by MCU day/night signal (code 0x0204).
    val isDark: Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs -> prefs[dayNightKey] ?: false }

    suspend fun setVariant(variant: HelmThemeVariant) {
        dataStore.edit { it[variantKey] = variant.name }
    }

    suspend fun setDark(dark: Boolean) {
        dataStore.edit { it[dayNightKey] = dark }
    }
}
