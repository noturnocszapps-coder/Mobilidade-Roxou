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
}
