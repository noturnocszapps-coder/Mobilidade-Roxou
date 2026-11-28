package br.com.roxou.mobilidade.overlay

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Central de diagnóstico do Radar. Guarda o estado real do serviço de leitura e um registro curto
 * de cada etapa (evento → oferta achada → leitura → cálculo → janela), com o motivo de cada descarte.
 * Nunca grava endereço, nome ou texto livre da tela: só números, tipos de linha e o motivo.
 */
object Diag {
    data class State(
        val serviceConnectedAt: Long = 0,
        val serviceAlive: Boolean = false,
        val lastEventAt: Long = 0,
        val lastEventApp: String = "",
        val eventsCount: Long = 0,
        val lastOfferSeenAt: Long = 0,
        val lastOfferApp: String = "",
        val lastOfferHow: String = "",
        val lastParse: String = "",
        val lastResult: String = "",
        val lastDiscard: String = "",
        val panel: String = "",
        val offersRead: Int = 0,
        val readFailures: Int = 0,
        val errors: Int = 0,
        val lastError: String = "",
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private const val FILE = "diagnostico.txt"
    private const val MAX_BYTES = 400_000L
    private val fmt = SimpleDateFormat("dd/MM HH:mm:ss", Locale.forLanguageTag("pt-BR"))
    private var filesDir: File? = null

    fun init(context: Context) { filesDir = context.filesDir }

    fun update(f: (State) -> State) { _state.value = f(_state.value) }

    /** Uma linha no registro. Use só para texto sem dados de passageiro. */
    @Synchronized
    fun log(msg: String) {
        val dir = filesDir ?: return
        runCatching {
            val file = File(dir, FILE)
            if (file.length() > MAX_BYTES) {
                val keep = file.readLines().takeLast(1500)
                file.writeText(keep.joinToString("\n", postfix = "\n"))
            }
            file.appendText("${fmt.format(Date())}  $msg\n")
        }
    }

    fun discard(reason: String) {
        update { it.copy(lastDiscard = reason, readFailures = it.readFailures + 1) }
        log("DESCARTE: $reason")
    }

    fun error(where: String, t: Throwable) {
        val msg = "$where: ${t.javaClass.simpleName} ${t.message?.take(120).orEmpty()}"
        update { it.copy(errors = it.errors + 1, lastError = msg) }
        log("ERRO $msg")
    }

    fun readLog(context: Context): String =
        runCatching { File(context.filesDir, FILE).takeIf { it.exists() }?.readText() }.getOrNull().orEmpty()

    fun clear(context: Context) {
        runCatching { File(context.filesDir, FILE).delete() }
        _state.value = _state.value.copy(readFailures = 0, errors = 0, lastError = "", lastDiscard = "", offersRead = 0)
    }

    /**
     * Esconde o que pode ser dado de passageiro numa linha do OCR: endereço, nome, bairro.
     * Fica só o que tem formato de número de oferta (valor, tempo, distância, nota, R$/km).
     */
    fun mask(text: String): String {
        val keep = Regex("""(?i)R\$\s?\d|\d+\s*min|\(\s*\d+[.,]?\d*\s*(km|m)\s*\)|\b[1-5][.,]\d{2}\b|/km|\bx\b|uber|99|aceitar|recusar|exclusivo|comfort|adolescente|black""")
        return if (keep.containsMatchIn(text)) text.replace(Regex("""(?i)\b(rua|r\.|av\.?|avenida)\b.*"""), "[endereço]")
        else "[texto ${text.length}]"
    }

    /** A leitura do Android (acessibilidade) está ligada para o Roxou nas Configurações? */
    fun accessibilityEnabled(context: Context): Boolean {
        val me = ComponentName(context, OfferReaderService::class.java)
        val list = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
        return list.split(':').any { ComponentName.unflattenFromString(it) == me }
    }
}
