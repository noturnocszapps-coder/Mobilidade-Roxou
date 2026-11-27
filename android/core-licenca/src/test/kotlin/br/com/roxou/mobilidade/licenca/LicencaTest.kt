package br.com.roxou.mobilidade.licenca

import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LicencaTest {
    private val par = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
    private val publica = Base64.getEncoder().encodeToString(par.public.encoded)
    private val url = Base64.getUrlEncoder().withoutPadding()
    private val validoAte = 2_000_000_000L

    private fun texto(modulos: String = "leitura,bairros,desconhecido", ate: Long = validoAte) =
        "v=1\nconta=c1\nemail=a@b.com\naparelho=cel-1\nplano=mensal\nmodulos=$modulos\n" +
            "valido_ate=$ate\ntolerancia_dias=7\nemitido_em=1764260000"

    private fun assinar(t: String): String {
        val s = Signature.getInstance("SHA256withECDSA").apply { initSign(par.private); update(t.toByteArray()) }.sign()
        return url.encodeToString(t.toByteArray()) + "." + url.encodeToString(s)
    }

    @Test fun licencaValidaConfere() {
        val lic = LicencaVerifier.verificar(assinar(texto()), publica)!!
        assertEquals(setOf(Modulo.LEITURA, Modulo.BAIRROS), lic.modulos)
        assertEquals("mensal", lic.plano)
        assertEquals(7, lic.toleranciaDias)
    }

    @Test fun textoAlteradoNaoConfere() {
        val assinatura = assinar(texto(modulos = "leitura")).substringAfter('.')
        val falso = url.encodeToString(texto(modulos = "leitura,backup").toByteArray())
        assertNull(LicencaVerifier.verificar("$falso.$assinatura", publica))
    }

    @Test fun outraChaveNaoConfere() {
        val outra = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        assertNull(LicencaVerifier.verificar(assinar(texto()), Base64.getEncoder().encodeToString(outra.public.encoded)))
        assertNull(LicencaVerifier.verificar("lixo", publica))
    }

    @Test fun aparelhoDiferenteFicaBloqueado() {
        val lic = LicencaVerifier.verificar(assinar(texto()), publica)
        assertTrue(Acesso.liberado(lic, Modulo.LEITURA, validoAte - 10, "cel-1"))
        assertFalse(Acesso.liberado(lic, Modulo.LEITURA, validoAte - 10, "cel-2"))
        assertFalse(Acesso.liberado(lic, Modulo.BACKUP, validoAte - 10, "cel-1"))
    }

    @Test fun toleranciaDeSeteDias() {
        val lic = LicencaVerifier.verificar(assinar(texto()), publica)
        val dia = 86_400L
        assertTrue(Acesso.liberado(lic, Modulo.LEITURA, validoAte + 3 * dia, "cel-1"))
        assertEquals(Situacao.EM_TOLERANCIA, Acesso.situacao(lic, validoAte + 3 * dia))
        assertEquals(4, Acesso.diasRestantes(lic, validoAte + 3 * dia))
        assertFalse(Acesso.liberado(lic, Modulo.LEITURA, validoAte + 8 * dia, "cel-1"))
        assertEquals(Situacao.VENCIDA, Acesso.situacao(lic, validoAte + 8 * dia))
    }

    @Test fun relogioVoltadoUsaOMaiorHorario() {
        val lic = LicencaVerifier.verificar(assinar(texto()), publica)
        val depoisDoFim = validoAte + 30 * 86_400L
        val agora = Acesso.agoraSeguro(agoraSeg = validoAte - 100, maiorJaVistoSeg = depoisDoFim)
        assertFalse(Acesso.liberado(lic, Modulo.LEITURA, agora, "cel-1"))
    }
}
