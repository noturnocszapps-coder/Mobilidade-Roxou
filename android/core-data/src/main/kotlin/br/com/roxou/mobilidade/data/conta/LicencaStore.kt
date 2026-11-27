package br.com.roxou.mobilidade.data.conta

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import br.com.roxou.mobilidade.data.BuildConfig
import br.com.roxou.mobilidade.licenca.Acesso
import br.com.roxou.mobilidade.licenca.Licenca
import br.com.roxou.mobilidade.licenca.LicencaVerifier
import br.com.roxou.mobilidade.licenca.Modulo
import br.com.roxou.mobilidade.licenca.Situacao
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** O que a tela de conta mostra. */
data class EstadoConta(
    val logado: Boolean,
    val email: String?,
    val plano: String,
    val modulos: Set<Modulo>,
    val validoAteSeg: Long?,
    val situacao: Situacao,
    val diasRestantes: Int,
    /** APK de teste sem chave configurada: tudo liberado. Nunca acontece no APK de venda. */
    val modoTeste: Boolean,
    val contaDisponivel: Boolean,
)

/**
 * Guarda a sessão e a licença assinada. A ÚNICA fonte do que está liberado é o token assinado pelo servidor:
 * nada de "plano" ou "liberado" solto nas preferências.
 */
class LicencaStore private constructor(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("roxou_licenca", Context.MODE_PRIVATE)

    @SuppressLint("HardwareIds")
    val aparelhoId: String = Settings.Secure.getString(app.contentResolver, Settings.Secure.ANDROID_ID) ?: "sem-id"

    /** Só no APK de teste (debug) e só se a chave pública não foi configurada. */
    val modoTeste: Boolean = BuildConfig.DEBUG && BuildConfig.CHAVE_PUBLICA.isBlank()
    private val apkOriginal: Boolean by lazy { BuildConfig.DEBUG || assinaturaOriginal(app) }

    private val _estado = MutableStateFlow(calcularEstado())
    val estado: StateFlow<EstadoConta> = _estado

    val sessao: String? get() = prefs.getString(K_SESSAO, null)

    @Volatile private var cache: Pair<String, Licenca?>? = null

    /** Confere a assinatura só quando o token muda (o serviço de leitura pergunta várias vezes por segundo). */
    fun licenca(): Licenca? {
        val token = prefs.getString(K_TOKEN, null) ?: return null
        cache?.let { (t, lic) -> if (t == token) return lic }
        return LicencaVerifier.verificar(token, BuildConfig.CHAVE_PUBLICA).also { cache = token to it }
    }

    private fun agora(): Long {
        val real = System.currentTimeMillis() / 1000
        val maior = prefs.getLong(K_MAIOR_HORA, 0)
        if (real > maior) prefs.edit().putLong(K_MAIOR_HORA, real).apply()
        return Acesso.agoraSeguro(real, maior)
    }

    fun liberado(modulo: Modulo): Boolean {
        if (modoTeste) return true
        if (!apkOriginal) return false
        return Acesso.liberado(licenca(), modulo, agora(), aparelhoId)
    }

    /** Licença emitida há mais de 6 horas (ou nenhuma): vale tentar renovar quando houver internet. */
    fun precisaRenovar(): Boolean {
        val lic = licenca() ?: return sessao != null
        return sessao != null && System.currentTimeMillis() / 1000 - lic.emitidoEmSeg > 6 * 3600
    }

    fun salvarLogin(sessao: String, token: String) {
        prefs.edit().putString(K_SESSAO, sessao).putString(K_TOKEN, token).apply()
        atualizar()
    }

    fun salvarLicenca(token: String) {
        prefs.edit().putString(K_TOKEN, token).apply()
        atualizar()
    }

    /** Sessão perdida (401): sai, mas a licença guardada continua valendo até vencer. */
    fun perderSessao() {
        prefs.edit().remove(K_SESSAO).apply()
        atualizar()
    }

    fun sair() {
        prefs.edit().remove(K_SESSAO).remove(K_TOKEN).apply()
        atualizar()
    }

    fun atualizar() { _estado.value = calcularEstado() }

    private fun calcularEstado(): EstadoConta {
        val lic = licenca()?.takeIf { it.aparelho == aparelhoId && apkOriginal }
        val agora = agora()
        val situacao = Acesso.situacao(lic, agora)
        val ativo = situacao == Situacao.EM_DIA || situacao == Situacao.EM_TOLERANCIA
        return EstadoConta(
            logado = sessao != null,
            email = lic?.email,
            plano = if (ativo) lic!!.plano else "gratis",
            modulos = if (modoTeste) Modulo.entries.toSet() else if (ativo) lic!!.modulos else emptySet(),
            validoAteSeg = lic?.validoAteSeg,
            situacao = situacao,
            diasRestantes = Acesso.diasRestantes(lic, agora),
            modoTeste = modoTeste,
            contaDisponivel = BuildConfig.API_URL.isNotBlank(),
        )
    }

    companion object {
        private const val K_SESSAO = "sessao"
        private const val K_TOKEN = "licenca"
        private const val K_MAIOR_HORA = "maior_hora"

        @Volatile private var instancia: LicencaStore? = null
        fun get(context: Context): LicencaStore =
            instancia ?: synchronized(this) { instancia ?: LicencaStore(context).also { instancia = it } }

        /**
         * Confere se o APK foi assinado com a chave do Roxou. Cópia refeita por outra pessoa
         * (outra assinatura) perde os módulos pagos.
         */
        @Suppress("DEPRECATION")
        fun assinaturaOriginal(context: Context): Boolean = runCatching {
            val esperado = BuildConfig.CERT_SHA256
            if (esperado.isBlank()) return false
            val pm = context.packageManager
            val certs = if (Build.VERSION.SDK_INT >= 28) {
                val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                info.signingInfo?.apkContentsSigners.orEmpty()
            } else {
                pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures.orEmpty()
            }
            certs.any { sig ->
                MessageDigest.getInstance("SHA-256").digest(sig.toByteArray()).joinToString("") { "%02x".format(it) } == esperado
            }
        }.getOrDefault(false)
    }
}
