package br.com.roxou.mobilidade.leitura.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Instância única, compartilhada entre as telas e o serviço de acessibilidade.
 * Antes cada um criava a sua, e o serviço nunca via o "Gravar agora" ligado nem a lista de pacotes atualizada.
 */
class PreferencesManager private constructor(context: Context) {
    companion object {
        @Volatile private var instance: PreferencesManager? = null
        fun get(context: Context): PreferencesManager =
            instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences("leitura_prefs", Context.MODE_PRIVATE)

    private val _isRecording = MutableStateFlow(prefs.getBoolean("is_recording", false))
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _maskPhones = MutableStateFlow(prefs.getBoolean("mask_phones", true))
    val maskPhones: StateFlow<Boolean> = _maskPhones.asStateFlow()

    private val _monitoredPackages = MutableStateFlow(
        prefs.getStringSet("monitored_packages", setOf("com.ubercab.driver", "com.app99.driver")) ?: setOf("com.ubercab.driver", "com.app99.driver")
    )
    val monitoredPackages: StateFlow<Set<String>> = _monitoredPackages.asStateFlow()

    private val _recentPackages = MutableStateFlow(
        prefs.getStringSet("recent_packages", emptySet()) ?: emptySet()
    )
    val recentPackages: StateFlow<Set<String>> = _recentPackages.asStateFlow()

    fun setRecording(enabled: Boolean) {
        prefs.edit().putBoolean("is_recording", enabled).apply()
        _isRecording.value = enabled
    }

    fun setMaskPhones(enabled: Boolean) {
        prefs.edit().putBoolean("mask_phones", enabled).apply()
        _maskPhones.value = enabled
    }

    fun setMonitoredPackages(packages: Set<String>) {
        prefs.edit().putStringSet("monitored_packages", packages).apply()
        _monitoredPackages.value = packages
    }

    fun addRecentPackage(pkg: String) {
        if (pkg.isBlank()) return
        val current = _recentPackages.value.toMutableSet()
        if (current.add(pkg)) {
            prefs.edit().putStringSet("recent_packages", current).apply()
            _recentPackages.value = current
        }
    }
}
