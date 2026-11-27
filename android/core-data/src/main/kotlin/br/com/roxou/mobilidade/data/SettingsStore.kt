package br.com.roxou.mobilidade.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.com.roxou.mobilidade.domain.settings.SettingsCodec
import br.com.roxou.mobilidade.domain.summary.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "roxou_settings")

/** Configurações no DataStore, todas como texto via [SettingsCodec]. */
class SettingsStore(context: Context) {
    private val store = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = store.data.map { prefs ->
        SettingsCodec.decode(prefs.asMap().entries.associate { (k, v) -> k.name to v.toString() })
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs ->
            val current = SettingsCodec.decode(prefs.asMap().entries.associate { (k, v) -> k.name to v.toString() })
            SettingsCodec.encode(transform(current)).forEach { (k, v) -> prefs[stringPreferencesKey(k)] = v }
        }
    }
}
