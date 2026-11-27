package br.com.roxou.mobilidade.data.conta

import br.com.roxou.mobilidade.data.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class PlanoApi(
    val id: String,
    val nome: String,
    val descricao: String,
    val precoCentavos: Long,
    val precoPrimeiraCentavos: Long?,
    /** 1, 3 ou 12. */
    val meses: Int,
)

data class Cobranca(val link: String?, val valorCentavos: Long, val pixCopiaECola: String?, val pixImagemBase64: String?)

class ErroApi(val status: Int, mensagem: String) : IOException(mensagem)

/** Fala com a API do Roxou (pasta server/ do repositório). Só HttpURLConnection + org.json, sem bibliotecas. */
class RoxouApi(private val store: LicencaStore, private val base: String = BuildConfig.API_URL.trimEnd('/')) {

    private suspend fun chamar(metodo: String, caminho: String, corpo: JSONObject? = null, autenticado: Boolean = false): JSONObject =
        withContext(Dispatchers.IO) {
            if (base.isBlank()) throw ErroApi(0, "Conta indisponível nesta versão.")
            val con = (URL(base + caminho).openConnection() as HttpURLConnection).apply {
                requestMethod = metodo
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("Content-Type", "application/json")
                if (autenticado) store.sessao?.let { setRequestProperty("Authorization", "Bearer $it") }
            }
            try {
                if (corpo != null) {
                    con.doOutput = true
                    con.outputStream.use { it.write(corpo.toString().toByteArray()) }
                }
                val status = con.responseCode
                val texto = (if (status in 200..299) con.inputStream else con.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(texto) }.getOrDefault(JSONObject())
                if (status == 401 && autenticado) store.perderSessao()
                if (status !in 200..299) throw ErroApi(status, json.optString("erro", "Erro $status. Tente de novo."))
                json
            } catch (e: ErroApi) {
                throw e
            } catch (e: IOException) {
                throw ErroApi(0, "Sem internet. Tente de novo.")
            } finally {
                con.disconnect()
            }
        }

    suspend fun planos(): Pair<Int, List<PlanoApi>> {
        val j = chamar("GET", "/v1/planos")
        val arr = j.getJSONArray("planos")
        val lista = (0 until arr.length()).map { i ->
            val p = arr.getJSONObject(i)
            PlanoApi(
                id = p.getString("id"),
                nome = p.getString("nome"),
                descricao = p.optString("descricao"),
                precoCentavos = p.getLong("preco_centavos"),
                precoPrimeiraCentavos = if (p.isNull("preco_primeira_centavos")) null else p.getLong("preco_primeira_centavos"),
                meses = when (p.getString("ciclo")) { "QUARTERLY" -> 3; "YEARLY" -> 12; else -> 1 },
            )
        }
        return j.optInt("diasTeste", 0) to lista
    }

    suspend fun pedirCodigo(email: String) {
        chamar("POST", "/v1/auth/codigo", JSONObject().put("email", email.trim()))
    }

    suspend fun entrar(email: String, codigo: String) {
        val j = chamar("POST", "/v1/auth/entrar",
            JSONObject().put("email", email.trim()).put("codigo", codigo).put("aparelho", store.aparelhoId))
        store.salvarLogin(j.getString("sessao"), j.getString("licenca"))
    }

    /** Renova a licença. Sem internet, não faz nada (o app segue com a licença guardada). */
    suspend fun renovarLicenca(): Boolean = runCatching {
        if (store.sessao == null) return false
        store.salvarLicenca(chamar("GET", "/v1/licenca", autenticado = true).getString("licenca"))
        true
    }.getOrDefault(false)

    suspend fun renovarSePreciso() { if (store.precisaRenovar()) renovarLicenca() }

    suspend fun assinar(plano: String, pix: Boolean, nome: String, cpf: String): Cobranca {
        val j = chamar("POST", "/v1/assinar", JSONObject()
            .put("plano", plano).put("forma", if (pix) "PIX" else "CREDIT_CARD").put("nome", nome).put("cpf", cpf),
            autenticado = true)
        val p = j.optJSONObject("pix")
        return Cobranca(j.optString("link").ifBlank { null }, j.optLong("valorCentavos"),
            p?.optString("copiaECola"), p?.optString("imagemBase64"))
    }

    suspend fun cancelar() { chamar("POST", "/v1/assinatura/cancelar", autenticado = true) }

    suspend fun sair() {
        runCatching { chamar("POST", "/v1/sair", autenticado = true) }
        store.sair()
    }
}
