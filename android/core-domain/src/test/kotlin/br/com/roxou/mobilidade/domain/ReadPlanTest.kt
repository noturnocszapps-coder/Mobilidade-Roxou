package br.com.roxou.mobilidade.domain

import br.com.roxou.mobilidade.domain.offer.ReadPlan
import br.com.roxou.mobilidade.domain.offer.ReadPlan.Action
import org.junit.Assert.assertEquals
import org.junit.Test

/** Oferta por cima da corrida (Uber e 99): o app nunca pode parar de ler (27/11). */
class ReadPlanTest {
    private fun plan(read: Boolean, tries: Int, sinceRead: Long = 0, rereads: Int = 0, sinceShot: Long = 0, recent: Boolean = false) =
        ReadPlan.decide(read, tries, sinceRead, rereads, sinceShot, recent)

    @Test fun primeiras_leituras_rapidas() {
        assertEquals(Action.FIRST_TRIES, plan(read = false, tries = 0))
        assertEquals(Action.FIRST_TRIES, plan(read = false, tries = 7))
    }

    @Test fun marca_do_cartao_na_tela_da_corrida_nao_faz_desistir() {
        // A tela da corrida tem a mesma marca do cartão: 8 leituras sem oferta.
        assertEquals(Action.GIVE_UP_NOTICE, plan(read = false, tries = 8))
        // Antes parava aqui para sempre. Agora segue lendo e pega a oferta nova que vier por cima.
        assertEquals(Action.KEEP_WATCHING, plan(read = false, tries = 9, sinceShot = 3_100))
        assertEquals(Action.KEEP_WATCHING, plan(read = false, tries = 9, sinceShot = 1_600, recent = true))
        assertEquals(Action.WAIT, plan(read = false, tries = 9, sinceShot = 1_000))
    }

    @Test fun oferta_lida_continua_conferindo_e_depois_vigia() {
        assertEquals(Action.REREAD, plan(read = true, tries = 1, sinceRead = 3_100, rereads = 2))
        // Depois de 10 conferências, não para: continua vigiando a tela.
        assertEquals(Action.KEEP_WATCHING, plan(read = true, tries = 1, sinceRead = 60_000, rereads = 10, sinceShot = 3_100))
    }

    @Test fun tela_buscando_da_99_e_fim_e_nao_e_aceite() {
        val lines = listOf("R$0,00", "0% de Taxa", "Solicitações (4) Nova", "Expirado. Renove para ganhar mais!", "Buscando")
            .mapIndexed { i, t -> br.com.roxou.mobilidade.domain.offer.OcrLine(t, 0, i * 100, 500, i * 100 + 40) }
        org.junit.Assert.assertTrue(br.com.roxou.mobilidade.domain.offer.AcceptDetector.looksEnded(lines))
        org.junit.Assert.assertFalse(br.com.roxou.mobilidade.domain.offer.AcceptDetector.looksAccepted(lines))
        org.junit.Assert.assertNull(br.com.roxou.mobilidade.domain.offer.OfferParser.parse(lines, br.com.roxou.mobilidade.domain.model.Platform.NINETY_NINE))
    }

    @Test fun telas_de_espera_da_uber_e_da_99() {
        fun ls(vararg t: String) = t.mapIndexed { i, x -> br.com.roxou.mobilidade.domain.offer.OcrLine(x, 0, i * 100, 500, i * 100 + 40) }
        val d = br.com.roxou.mobilidade.domain.offer.AcceptDetector
        org.junit.Assert.assertTrue(d.looksIdle(ls("+R$ 3,25", "1-12 min", "Você está online", "O passe de 24 horas está ativo")))
        org.junit.Assert.assertTrue(d.looksIdle(ls("Procurando viagens")))
        org.junit.Assert.assertTrue(d.looksIdle(ls("Solicitações (4) Nova", "Buscando")))
        org.junit.Assert.assertFalse(d.looksIdle(ls("5 min", "2,3 km", "Destino de Maria")))
    }

    /** Recusou no X da Uber/99: a tela volta para a espera. Mesmo com a rua do embarque no mapa, não é aceite. */
    @Test fun recusou_e_voltou_para_espera_nao_e_aceite() {
        fun ls(vararg t: String) = t.mapIndexed { i, x -> br.com.roxou.mobilidade.domain.offer.OcrLine(x, 0, i * 100, 500, i * 100 + 40) }
        val d = br.com.roxou.mobilidade.domain.offer.AcceptDetector
        val uber = ls("Rua Doutor José Foz", "+R$ 1", "Você está online")
        org.junit.Assert.assertTrue(d.looksIdle(uber))
        org.junit.Assert.assertFalse(d.looksAccepted(uber))
        val noventaENove = ls("Rua Doutor José Foz", "Solicitações (4)", "Buscando")
        org.junit.Assert.assertTrue(d.looksIdle(noventaENove))
        org.junit.Assert.assertFalse(d.looksAccepted(noventaENove))
    }
}
