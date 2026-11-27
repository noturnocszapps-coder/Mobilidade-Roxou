package br.com.roxou.mobilidade.overlay

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Liga/desliga do "Radar de corridas" com um toque, sem mexer na acessibilidade do Android.
 * Desligado: o serviço continua ativo, mas não lê nada e a janela some.
 */
object Radar {
    private const val PREFS = "roxou_overlay"
    private const val KEY = "radar_on"
    private val state = MutableStateFlow<Boolean?>(null)

    fun flow(context: Context): StateFlow<Boolean?> {
        if (state.value == null) state.value = isOn(context)
        return state
    }

    fun isOn(context: Context): Boolean =
        state.value ?: context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, true).also { state.value = it }

    fun set(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply()
        state.value = on
    }
}
