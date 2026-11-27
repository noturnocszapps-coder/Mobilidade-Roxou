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
        listOf("UberX", "Comfort", "Business Comfort", "Exclusivo", "Uber Adolescente", "Black").forEach { h ->
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
        assertEquals(Platform.UBER, OfferParser.platformHint(listOf(l("Business Comfort", 1))))
        assertNull(OfferParser.platformHint(listOf(l("Rua Tal, 10", 1))))
    }

    /** Tela real da 99 depois do aceite (print do Fernando, 27/11 19:43): navegação até o embarque. */
    private val navegacao99 = listOf(
        l("457 m", 180), l("Rotatória", 280),
        l("Shopping Pão, Avenida Ademar de", 420), l("Barros, 830, Jardim Aviação", 470),
        l("Ir", 590), l("Street View", 760), l("2 min · 662 m", 1740), l("Chegue antes de 19:48", 1800),
    )

    @Test fun aceite_na_99_pela_navegacao() {
        val d = br.com.roxou.mobilidade.domain.offer.AcceptDetector
        assertTrue(d.looksAccepted(navegacao99))
        assertTrue(d.mentionsStreet(navegacao99, "Shopping Pão, Avenida Ademar de Barros, 830"))
        assertTrue(d.mentionsStreet(listOf(l("Rua Bartira, 110, Vila Maristela", 400)), "Rua Bartira, 110 , Vila Maristela"))
        assertFalse(d.mentionsStreet(navegacao99, "Rua João da Silva Espinhosa, 12, Uep5-S.2"))
    }

    @Test fun oferta_da_99_com_parada_e_negocia() {
        val parada = listOf(
            l("Dinheiro", 470, left = 87), l("R$11,40 1,1x", 590, left = 67, h = 100), l("R$1,62/km", 720, left = 67),
            l("0% de Taxa99 semanal", 860, left = 130), l("R$1,97 Tarifa base dinâmica incl.", 940, left = 130),
            l("Taxa de parada de R$0,70 inclusa", 1020, left = 130), l("4,94 · 768 corridas", 1160, left = 130),
            l("5 min (1,5 km)", 1370, left = 130), l("Rua São Sebastião, 517, Vila", 1420, left = 130), l("Machadinho", 1470, left = 130),
            l("1 parada(s)", 1550, left = 130), l("11 min (5,5 km)", 1630, left = 130),
            l("Rua João da Silva Espinhosa, 12,", 1690, left = 130), l("Uep5-S.2", 1745, left = 130),
        )
        val o = OfferParser.parse(parada, Platform.NINETY_NINE)!!
        assertEquals(11_40L, o.fareCents)
        assertEquals(1.5 + 5.5, o.pickupKm + o.tripKm, 1e-9)
        assertEquals(4.94, o.passengerRating!!, 1e-9)
        val e = OfferEvaluator.evaluate(o, OfferRules())
        assertEquals(163L, e.perKmCents)
        assertEquals(16, e.totalMinutes)
        val negocia = listOf(
            l("Negocia · Dinheiro", 550, left = 87), l("R$9,67", 680, left = 67, h = 110),
            l("4,96 · 313 corridas", 1010, left = 130), l("8 min (2,7 km)", 1150, left = 130),
            l("José Rainho Teixeira, 109 , Vila", 1200, left = 130), l("Marcondes", 1255, left = 130),
            l("13 min (7,5 km)", 1330, left = 130), l("Rua Francisco dos Santos", 1380, left = 130),
            l("Cavalheiro, 63, Uep5-S.2", 1435, left = 130), l("Aceitar por R$9,67", 1560, left = 270),
            l("R$10,15 R$10,44 R$10,64", 1700, left = 70),
        )
        val n = OfferParser.parse(negocia, Platform.NINETY_NINE)!!
        assertEquals(9_67L, n.fareCents)
        assertEquals(95L, OfferEvaluator.evaluate(n, OfferRules()).perKmCents)
    }

    /** Print do Fernando (27/11): "4 min (1.5 km)" lido como "4 min (15 km)" deu R$ 0,62/km. */
    @Test fun uber_ponto_decimal_perdido_no_ocr() {
        val lines = listOf(
            l("UberX", 1000), l("R$ 12,02", 1100, h = 120), l("* 5,00 (117)", 1260),
            l("4 min (15 km)", 1420), l("R. Tal, 10, Presidente Prudente", 1480),
            l("10 minutos (4.5 km)", 1620), l("Av. Outra, 20, Presidente Prudente", 1680),
            l("Tenho interesse", 1990),
        )
        val o = OfferParser.parse(lines, Platform.UBER)!!
        assertEquals(1.5, o.pickupKm, 1e-9)
        assertEquals(4.5, o.tripKm, 1e-9)
        val e = OfferEvaluator.evaluate(o, OfferRules())
        assertEquals(6.0, e.totalKm, 1e-9)
        assertEquals(14, e.totalMinutes)
        assertEquals(200L, e.perKmCents)
        assertFalse(e.timeEstimated)
    }

    /** Print do Fernando (27/11): esta saiu certa e tem que continuar igual. */
    @Test fun uber_com_ponto_lido_certo_nao_muda() {
        val lines = listOf(
            l("UberX", 1000), l("R$ 21,06", 1100, h = 120), l("* 4,95 (80)", 1260),
            l("10 min (5.9 km)", 1420), l("R. Tal, 10, Presidente Prudente", 1480),
            l("15 minutos (8.5 km)", 1620), l("Av. Outra, 20, Presidente Prudente", 1680),
            l("Tenho interesse", 1990),
        )
        val e = OfferEvaluator.evaluate(OfferParser.parse(lines, Platform.UBER)!!, OfferRules())
        assertEquals(14.4, e.totalKm, 1e-9)
        assertEquals(25, e.totalMinutes)
        assertEquals(146L, e.perKmCents)
        assertEquals(50_54L, e.perHourCents)
    }

    /** Print do Fernando (27/11): Comfort + Exclusivo, botão "Aceitar", endereço em 3 linhas. */
    @Test fun uber_comfort_exclusivo_com_botao_aceitar() {
        val lines = listOf(
            l("Comfort", 950, left = 165), l("Exclusivo", 950, left = 455), l("R$ 9,08", 1050, left = 96, h = 110),
            l("4,95 (23)", 1205, left = 190), l("Verificado", 1205, left = 530),
            l("6 min (2.5 km)", 1360), l("Rua Miguel Verderezzi di Colla,", 1430), l("Jardim Everest, Presidente", 1490),
            l("Prudente", 1550), l("5 minutos (1.6 km)", 1625), l("Rua Ricardo Anderson Cristófano,", 1695),
            l("385, Parque Residencial Carandá,", 1755), l("Presidente Prudente", 1815), l("Aceitar", 1990, left = 545),
        )
        assertEquals(Platform.UBER, OfferParser.platformHint(lines))
        val o = OfferParser.parse(lines, Platform.UBER)!!
        assertEquals(9_08L, o.fareCents)
        assertEquals(2.5, o.pickupKm, 1e-9)
        assertEquals(1.6, o.tripKm, 1e-9)
        assertEquals(11, o.pickupMinutes + o.tripMinutes)
        assertTrue(o.dropoffAddress!!.startsWith("Rua Ricardo Anderson Cristófano, 385"))
    }
}
