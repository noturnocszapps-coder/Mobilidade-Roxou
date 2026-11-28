package br.com.roxou.mobilidade.licenca

import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/** Funções pagas. A lista de cada plano vem do servidor (tabela mobilidade.planos). */
enum class Modulo(val chave: String, val nome: String) {
    LEITURA("leitura", "Leitura automática das ofertas e janela flutuante"),
    BAIRROS("bairros", "Filtro de bairros"),
    HISTORICO("historico", "Histórico completo"),
    BACKUP("backup", "Backup na nuvem"),
    RELATORIOS("relatorios", "Relatórios"),
    ;

    companion object {
        fun daChave(chave: String): Modulo? = entries.firstOrNull { it.chave == chave.trim() }
    }
}

data class Licenca(
    val conta: String,
    val email: String,
    val aparelho: String,
    val plano: String,
    val modulos: Set<Modulo>,
    val validoAteSeg: Long,
    val toleranciaDias: Int,
    val emitidoEmSeg: Long,
)

/**
 * Confere a licença assinada pelo servidor, sem internet.
 * Token = base64url(texto) + "." + base64url(assinatura ECDSA P-256/SHA-256 em DER).
 * Texto = linhas "chave=valor". Só o servidor tem a chave privada, então ninguém consegue fabricar uma licença.
 */
object LicencaVerifier {
    fun verificar(token: String, chavePublicaBase64: String): Licenca? = runCatching {
        val partes = token.trim().split('.')
        if (partes.size != 2 || chavePublicaBase64.isBlank()) return null
        val dec = Base64.getUrlDecoder()
        val texto = dec.decode(partes[0])
        val chave = KeyFactory.getInstance("EC")
            .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(chavePublicaBase64.trim())))
        val sig = Signature.getInstance("SHA256withECDSA").apply { initVerify(chave); update(texto) }
        if (!sig.verify(dec.decode(partes[1]))) return null

        val campos = String(texto, Charsets.UTF_8).lines().mapNotNull { linha ->
            val i = linha.indexOf('=')
            if (i <= 0) null else linha.substring(0, i) to linha.substring(i + 1)
        }.toMap()
        if (campos["v"] != "1") return null
        Licenca(
            conta = campos.getValue("conta"),
            email = campos["email"].orEmpty(),
            aparelho = campos.getValue("aparelho"),
            plano = campos.getValue("plano"),
            modulos = campos["modulos"].orEmpty().split(',').mapNotNull(Modulo::daChave).toSet(),
            validoAteSeg = campos.getValue("valido_ate").toLong(),
            toleranciaDias = campos.getValue("tolerancia_dias").toInt(),
            emitidoEmSeg = campos.getValue("emitido_em").toLong(),
        )
    }.getOrNull()
}

enum class Situacao { EM_DIA, EM_TOLERANCIA, VENCIDA, SEM_LICENCA }

object Acesso {
    private const val DIA = 86_400L

    /** Proteção contra voltar o relógio do celular: usa o maior horário que o app já viu. */
    fun agoraSeguro(agoraSeg: Long, maiorJaVistoSeg: Long): Long = maxOf(agoraSeg, maiorJaVistoSeg)

    fun liberado(lic: Licenca?, modulo: Modulo, agoraSeg: Long, aparelho: String): Boolean =
        lic != null && lic.aparelho == aparelho && modulo in lic.modulos &&
            agoraSeg <= lic.validoAteSeg + lic.toleranciaDias * DIA

    fun situacao(lic: Licenca?, agoraSeg: Long): Situacao = when {
        lic == null -> Situacao.SEM_LICENCA
        agoraSeg <= lic.validoAteSeg -> Situacao.EM_DIA
        agoraSeg <= lic.validoAteSeg + lic.toleranciaDias * DIA -> Situacao.EM_TOLERANCIA
        else -> Situacao.VENCIDA
    }

    /** Dias até o fim do período pago (ou da folga, se já estiver nela). Nunca negativo. */
    fun diasRestantes(lic: Licenca?, agoraSeg: Long): Int {
        lic ?: return 0
        val fim = if (agoraSeg <= lic.validoAteSeg) lic.validoAteSeg else lic.validoAteSeg + lic.toleranciaDias * DIA
        return ((fim - agoraSeg + DIA - 1) / DIA).coerceAtLeast(0).toInt()
    }
}
