package br.com.roxou.mobilidade.domain

import br.com.roxou.mobilidade.domain.model.Platform
import br.com.roxou.mobilidade.domain.offer.OcrLine
import br.com.roxou.mobilidade.domain.offer.OfferEvaluator
import br.com.roxou.mobilidade.domain.offer.OfferParser
import br.com.roxou.mobilidade.domain.offer.OfferRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Casos do relatório de auditoria do radar (27/11/2026). */
class RadarAuditTest {
    private fun l(t: String, top: Int, left: Int = 196, h: Int = 45) = OcrLine(t, left, top, left + 600, top + h)

    private fun uber(pickup: String, trip: String, header: String = "UberX") = listOf(
        l("R$ 0,00", 120, left = 420, h = 40), // contador de ganhos do topo da Uber
        l(header, 1069),
        l("R$ 16,58", 1165, left = 100, h = 124),
        l("* 4,95 (120)", 1327, left = 132),
        l(pickup, 1486),
        l("R. Luiz Antonio Chelse, Presidente", 1561),
        l("Prudente", 1621),
        l(trip, 1687),
        l("Av. Washington Luiz, 875,", 1765),
        l("Presidente Prudente", 1826),
        l("Aceitar", 1993, left = 438),
    )

    @Test fun r_por_hora_conta_busca_e_viagem() {
        val o = OfferParser.parse(uber("13 min (12 km)", "10 min (3.6 km)"), Platform.UBER)!!
        assertEquals(16_58L, o.fareCents)
        assertFalse(o.timeSuspect)
        val e = OfferEvaluator.evaluate(o, OfferRules())
        assertEquals(23, e.totalMinutes)
        assertEquals(106L, e.perKmCents) // R$ 16,58 / 15,6 km
        assertEquals(43_25L, e.perHourCents) // R$ 16,58 em 23 min
        assertFalse(e.timeEstimated)
    }

    @Test fun tempo_lido_sem_sentido_vira_estimativa() {
        // O OCR perdeu o "1" de "13 min": 12 km em 3 min seria 240 km/h.
        val o = OfferParser.parse(uber("3 min (12 km)", "10 min (3.6 km)"), Platform.UBER)!!
        assertTrue(o.timeSuspect)
        val e = OfferEvaluator.evaluate(o, OfferRules())
        assertTrue(e.timeEstimated)
        assertEquals(24 + 10, e.totalMinutes) // 12 km a 30 km/h = 24 min
        assertTrue(e.perHourCents < 30_00)
    }

    @Test fun variantes_da_uber_sao_lidas() {
        listOf("UberX", "Comfort", "Exclusivo", "Uber Adolescente", "Black").forEach { h ->
            val o = OfferParser.parse(uber("13 min (12 km)", "10 minutos (3.6 km)", h), Platform.UBER)
            assertEquals(h, 16_58L, o?.fareCents)
        }
    }

    @Test fun cartao_com_aviso_de_incluido_e_valor_por_km() {
        val lines = uber("5 min (1.8 km)", "12 min (4.2 km)", "Exclusivo") + listOf(
            l("+R$ 2,00 incluído", 1250, left = 100, h = 40),
            l("R$ 2,74/km", 1290, left = 520, h = 40),
        )
        val o = OfferParser.parse(lines, Platform.UBER)!!
        assertEquals(16_58L, o.fareCents)
    }

    @Test fun descobre_o_app_pelo_texto() {
        assertEquals(Platform.UBER, OfferParser.platformHint(listOf(l("UberX", 1), l("Exclusivo", 2))))
        assertEquals(Platform.UBER, OfferParser.platformHint(listOf(l("Uber Adolescente", 1))))
        assertEquals(Platform.NINETY_NINE, OfferParser.platformHint(listOf(l("0% de Taxa99 semanal", 1))))
        assertNull(OfferParser.platformHint(listOf(l("Rua Tal, 10", 1))))
    }
}
