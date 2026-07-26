package dev.helm.themes

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "helm_theme")

class ThemeRepository(context: Context) {

    private val dataStore = context.applicationContext.themeDataStore
    private val variantKey = stringPreferencesKey("variant")

    val selectedVariant: Flow<HelmThemeVariant> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            runCatching { HelmThemeVariant.valueOf(prefs[variantKey] ?: "") }
                .getOrDefault(HelmThemeVariant.HELM)
        }

    suspend fun setVariant(variant: HelmThemeVariant) {
        dataStore.edit { it[variantKey] = variant.name }
    }
}
