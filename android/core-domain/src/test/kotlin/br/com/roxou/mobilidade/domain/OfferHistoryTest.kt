package br.com.roxou.mobilidade.domain

import br.com.roxou.mobilidade.domain.model.Platform
import br.com.roxou.mobilidade.domain.offer.AreaMatch
import br.com.roxou.mobilidade.domain.offer.OcrLine
import br.com.roxou.mobilidade.domain.offer.OfferEvaluator
import br.com.roxou.mobilidade.domain.offer.OfferOutcome
import br.com.roxou.mobilidade.domain.offer.OfferParser
import br.com.roxou.mobilidade.domain.offer.OfferRecord
import br.com.roxou.mobilidade.domain.offer.OfferRecordCodec
import br.com.roxou.mobilidade.domain.offer.OfferRules
import br.com.roxou.mobilidade.domain.offer.OfferStatsEngine
import br.com.roxou.mobilidade.domain.offer.ParsedOffer
import br.com.roxou.mobilidade.domain.offer.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class OfferHistoryTest {
    private fun line(t: String, top: Int) = OcrLine(t, 100, top, 500, top + 40)

    @Test fun le_nota_da_99_e_da_uber() {
        assertEquals(4.88, OfferParser.findRating(listOf(line("t 4,88 986 corridas", 10)))!!, 1e-9)
        assertEquals(4.87, OfferParser.findRating(listOf(line("4,87 · 31 corridas", 10)))!!, 1e-9)
        assertEquals(5.0, OfferParser.findRating(listOf(line("* 5,00 (48)", 10)))!!, 1e-9)
        assertNull(OfferParser.findRating(listOf(line("R$13,80 41,1x", 10), line("R$1,64/km", 60))))
    }

    private val base = ParsedOffer(Platform.UBER, 40_00, 1.0, 3, 9.0, 15)

    @Test fun nota_baixa_piora_a_cor_e_avisa() {
        val boa = OfferEvaluator.evaluate(base.copy(passengerRating = 4.95), OfferRules())
        assertEquals(Verdict.GOOD, boa.verdict)
        assertFalse(boa.lowRating)
        val baixa = OfferEvaluator.evaluate(base.copy(passengerRating = 4.70), OfferRules())
        assertEquals(Verdict.BORDERLINE, baixa.verdict)
        assertTrue(baixa.lowRating)
        assertTrue(baixa.reason.startsWith("Nota do passageiro 4,70"))
        val desligado = OfferEvaluator.evaluate(base.copy(passengerRating = 4.70), OfferRules(minPassengerRating = 0.0))
        assertFalse(desligado.lowRating)
    }

    @Test fun grava_e_le_oferta_e_ultima_linha_vale() {
        val e = OfferEvaluator.evaluate(base.copy(passengerRating = 4.9), OfferRules())
        val r = OfferRecord.from("a1", Instant.ofEpochMilli(1_000), e, OfferOutcome.REJECTED)
        val back = OfferRecordCodec.decode(OfferRecordCodec.encode(r))
        assertEquals(r, back)
        val all = OfferRecordCodec.decodeAll(sequenceOf(
            OfferRecordCodec.encode(r), "lixo", OfferRecordCodec.encode(r.copy(outcome = OfferOutcome.ACCEPTED)),
        ))
        assertEquals(1, all.size)
        assertEquals(OfferOutcome.ACCEPTED, all[0].outcome)
    }

    @Test fun resumo_por_dia_e_taxa_de_aceite() {
        val zone = ZoneId.of("America/Sao_Paulo")
        val e = OfferEvaluator.evaluate(base, OfferRules())
        fun at(d: String, h: Int) = LocalDate.parse(d).atTime(h, 0).atZone(zone).toInstant()
        val recs = listOf(
            OfferRecord.from("1", at("2026-11-23", 8), e, OfferOutcome.ACCEPTED), // segunda
            OfferRecord.from("2", at("2026-11-23", 8), e, OfferOutcome.REJECTED),
            OfferRecord.from("3", at("2026-11-23", 9), e, OfferOutcome.REJECTED),
            OfferRecord.from("4", at("2026-11-27", 18), e, OfferOutcome.ACCEPTED), // sexta
        )
        val s = OfferStatsEngine.compute(recs, zone, LocalDate.parse("2026-11-27"))
        assertEquals(4, s.total)
        assertEquals(2, s.accepted)
        assertEquals(0.5, s.acceptRate, 1e-9)
        assertEquals(2.0, s.perDay, 1e-9)
        assertEquals(1, s.todayTotal)
        assertEquals(DayOfWeek.MONDAY, s.bestWeekday)
        assertEquals(8, s.busiestHour)
    }

    @Test fun etapas_da_corrida() {
        val aCaminho = listOf(line("A caminho do passageiro", 10), line("Iniciar viagem", 60))
        assertTrue(br.com.roxou.mobilidade.domain.offer.AcceptDetector.looksAccepted(aCaminho))
        assertFalse(br.com.roxou.mobilidade.domain.offer.AcceptDetector.looksOnTrip(aCaminho))
        assertTrue(br.com.roxou.mobilidade.domain.offer.AcceptDetector.looksOnTrip(listOf(line("Deslize para finalizar", 10))))
        assertTrue(br.com.roxou.mobilidade.domain.offer.AcceptDetector.looksEnded(listOf(line("Avalie o passageiro", 10))))
    }
}
