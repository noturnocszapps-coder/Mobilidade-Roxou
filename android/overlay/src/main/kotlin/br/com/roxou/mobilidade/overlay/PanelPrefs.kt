package br.com.roxou.mobilidade.overlay

import android.content.Context

/** Como a janela flutuante aparece. Fica no celular (SharedPreferences "roxou_overlay"). */
object PanelPrefs {
    private const val PREFS = "roxou_overlay"
    enum class Position(val label: String) { LEFT("Esquerda"), CENTER("Centro"), RIGHT("Direita") }

    val SECONDS_OPTIONS = listOf(2, 3, 5, 10)

    private fun p(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun position(c: Context): Position =
        runCatching { Position.valueOf(p(c).getString("position", null) ?: "") }.getOrDefault(Position.LEFT)
    fun setPosition(c: Context, v: Position) = p(c).edit().putString("position", v.name).apply()

    /** Quanto tempo a oferta fica na tela antes de sumir sozinha. */
    fun seconds(c: Context): Int = p(c).getInt("seconds", 5)
    fun setSeconds(c: Context, v: Int) = p(c).edit().putInt("seconds", v).apply()

    /** Depois do aceite, deixa um botãozinho do Waze (embarque/destino) na tela. */
    fun wazeAfterAccept(c: Context): Boolean = p(c).getBoolean("waze_after_accept", true)
    fun setWazeAfterAccept(c: Context, v: Boolean) = p(c).edit().putBoolean("waze_after_accept", v).apply()
}
