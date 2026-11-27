package br.com.roxou.mobilidade.domain

import br.com.roxou.mobilidade.domain.fuel.FuelEngine
import br.com.roxou.mobilidade.domain.fuel.FuelLog
import br.com.roxou.mobilidade.domain.journey.Journey
import br.com.roxou.mobilidade.domain.journey.JourneyEngine
import br.com.roxou.mobilidade.domain.journey.JourneyPause
import br.com.roxou.mobilidade.domain.journey.JourneyStatus
import br.com.roxou.mobilidade.domain.journey.OdometerCheckpoint
import br.com.roxou.mobilidade.domain.km.KmAllowanceEngine
import br.com.roxou.mobilidade.domain.km.KmRecord
import br.com.roxou.mobilidade.domain.km.KmStatus
import br.com.roxou.mobilidade.domain.restday.DayResult
import br.com.roxou.mobilidade.domain.restday.RestDayEngine
import br.com.roxou.mobilidade.domain.time.Turnover
import br.com.roxou.mobilidade.domain.trips.ManualTripEntry
import br.com.roxou.mobilidade.domain.trips.ManualTripForm
import br.com.roxou.mobilidade.domain.trips.ManualTripResult
import br.com.roxou.mobilidade.domain.model.Platform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

/** Cenários vindos de `scripts/test_*.ts` do Lucro App e casos novos de km e lançamento manual. */
class EnginesPortTest {

    private fun fuel(id: String, day: String, odo: Double, liters: Double, full: Boolean) =
        FuelLog(id, "veh", br("${day}T10:00"), odo, liters, (liters * 500).toLong(), full)

    @Test fun `combustivel - ciclo tanque cheio a tanque cheio`() {
        val r = FuelEngine.fullToFullConsumption(
            listOf(fuel("a", "2026-10-10", 40000.0, 45.0, true), fuel("b", "2026-10-14", 40500.0, 50.0, true)), "veh",
        )
        assertTrue(r.hasValidMeasurements)
        assertEquals(10.0, r.cycles.single().kmPerL, 1e-9)
    }

    @Test fun `combustivel - parcial intermediario soma litros`() {
        val r = FuelEngine.fullToFullConsumption(
            listOf(
                fuel("p0", "2026-10-01", 50000.0, 45.0, true),
                fuel("p1", "2026-10-03", 50200.0, 15.0, false),
                fuel("p2", "2026-10-06", 50600.0, 35.0, true),
            ),
        )
        val c = r.cycles.single()
        assertEquals(600.0, c.distance, 1e-9)
        assertEquals(50.0, c.liters, 1e-9)
        assertEquals(12.0, c.kmPerL, 1e-9)
    }

    @Test fun `combustivel - odometro regressivo e consumo impossivel sao ignorados`() {
        val r = FuelEngine.fullToFullConsumption(
            listOf(fuel("a", "2026-10-01", 50000.0, 40.0, true), fuel("b", "2026-10-02", 49000.0, 40.0, true)),
        )
        assertFalse(r.hasValidMeasurements)
        val r2 = FuelEngine.fullToFullConsumption(
            listOf(fuel("a", "2026-10-01", 50000.0, 40.0, true), fuel("b", "2026-10-02", 55000.0, 40.0, true)),
        )
        assertFalse(r2.hasValidMeasurements) // 125 km/l
    }

    @Test fun `jornada - tempo efetivo desconta pausas`() {
        val j = Journey(
            "j", LocalDate.of(2026, 11, 27), br("2026-11-27T15:00"), br("2026-11-27T23:00"), JourneyStatus.COMPLETED,
            pauses = listOf(JourneyPause("p", br("2026-11-27T19:00"), br("2026-11-27T19:30"))),
        )
        val b = JourneyEngine.breakdown(j, br("2026-11-28T10:00"))
        assertEquals(8 * 3600L, b.grossSeconds)
        assertEquals(30 * 60L, b.pauseSeconds)
        assertEquals(7.5, b.effectiveHours, 1e-9)
    }

    @Test fun `jornada - pausa esquecida e corrigida guardando o original`() {
        val j = Journey(
            "j", LocalDate.of(2026, 11, 27), br("2026-11-27T15:00"), null, JourneyStatus.PAUSED,
            pauses = listOf(JourneyPause("p", br("2026-11-27T19:00"))),
        )
        val now = br("2026-11-27T21:00")
        assertTrue(JourneyEngine.isProlongedPause(j, now))
        val fixed = JourneyEngine.correctForgottenPause(j, "p", br("2026-11-27T19:20"), now)
        assertEquals(JourneyStatus.ACTIVE, fixed.status)
        assertEquals(now, fixed.pauses.single().originalResumedAt)
        assertEquals(20 * 60L, JourneyEngine.breakdown(fixed, now).pauseSeconds)
    }

    @Test fun `jornada - validacao de horarios`() {
        val s = br("2026-11-27T15:00")
        assertTrue(JourneyEngine.validate(s, s.minusSeconds(1), emptyList()) is JourneyEngine.Validation.Invalid)
        val overlap = listOf(
            JourneyPause("a", s.plusSeconds(600), s.plusSeconds(1800)),
            JourneyPause("b", s.plusSeconds(1200), s.plusSeconds(2400)),
        )
        assertTrue(JourneyEngine.validate(s, s.plusSeconds(7200), overlap) is JourneyEngine.Validation.Invalid)
        assertEquals(JourneyEngine.Validation.Valid, JourneyEngine.validate(s, s.plusSeconds(7200), overlap.take(1)))
    }

    @Test fun `franquia - jornada que cruza segunda 0h com odometro na virada`() {
        val t = Turnover()
        val week = t.kmWeek(br("2026-11-30T10:00"))
        val j = Journey(
            "j", LocalDate.of(2026, 11, 29), br("2026-11-29T20:00"), br("2026-11-30T03:00"), JourneyStatus.COMPLETED,
            startOdometerKm = 1000.0, endOdometerKm = 1200.0,
            checkpoints = listOf(OdometerCheckpoint(br("2026-11-30T00:00"), 1150.0)),
        )
        val p = KmAllowanceEngine.progress(week, 1500, 0, emptyList(), listOf(j))
        assertEquals(50.0, p.workKm, 1e-9)
        assertNull(p.pendingAttributionJourneyId)
        val prev = KmAllowanceEngine.progress(week.previous(), 1500, 0, emptyList(), listOf(j))
        assertEquals(150.0, prev.workKm, 1e-9)
    }

    @Test fun `franquia - sem odometro na virada fica pendente`() {
        val t = Turnover()
        val week = t.kmWeek(br("2026-11-30T10:00"))
        val j = Journey(
            "j", LocalDate.of(2026, 11, 29), br("2026-11-29T20:00"), br("2026-11-30T03:00"), JourneyStatus.COMPLETED,
            startOdometerKm = 1000.0, endOdometerKm = 1200.0,
        )
        val p = KmAllowanceEngine.progress(week, 1500, 0, emptyList(), listOf(j))
        assertEquals(0.0, p.workKm, 1e-9)
        assertEquals("j", p.pendingAttributionJourneyId)
    }

    @Test fun `franquia - excedente e status`() {
        val week = Turnover().kmWeek(br("2026-11-25T10:00"))
        val p = KmAllowanceEngine.progress(
            week, 1167, 71,
            listOf(KmRecord("a", br("2026-11-24T10:00"), 1100.0, 100.0)),
        )
        assertEquals(KmStatus.LIMIT_REACHED, p.status)
        assertEquals(33.0, p.exceededKm, 1e-9)
        assertEquals(23_43L, p.exceededCostCents)
        val att = KmAllowanceEngine.progress(week, 1500, 0, listOf(KmRecord("a", br("2026-11-24T10:00"), 1250.0)))
        assertEquals(KmStatus.ATTENTION, att.status)
    }

    @Test fun `folga - menor R$ por hora liquido`() {
        val monday = LocalDate.of(2026, 11, 2)
        val history = (0L..13L).map { i ->
            val d = monday.plusDays(i)
            val gross = when (d.dayOfWeek) { DayOfWeek.TUESDAY -> 150_00L; DayOfWeek.FRIDAY -> 530_00L; else -> 300_00L }
            DayResult(d, worked = true, grossCents = gross, costsCents = 50_00, hours = 8.0, km = 150.0)
        }
        val r = RestDayEngine.recommend(history)
        assertTrue(r.hasSufficientData)
        assertEquals(DayOfWeek.TUESDAY, r.best)
        assertFalse(RestDayEngine.recommend(history.take(4)).hasSufficientData)
    }

    @Test fun `lancamento manual - valores em formato brasileiro`() {
        assertEquals(1250L, ManualTripEntry.parseMoneyToCents("12,50"))
        assertEquals(1250L, ManualTripEntry.parseMoneyToCents("12.50"))
        assertEquals(123456L, ManualTripEntry.parseMoneyToCents("R$ 1.234,56"))
        assertNull(ManualTripEntry.parseMoneyToCents("abc"))
        val now = br("2026-11-27T20:00")
        val ok = ManualTripEntry.build(ManualTripForm(Platform.UBER, "18,90", now, tripKm = "7,5", pickupKm = "1.2"), "id1", now)
        ok as ManualTripResult.Ok
        assertEquals(1890L, ok.trip.amountCents)
        assertEquals(8.7, ok.trip.totalKm!!, 1e-9)
        assertTrue(ManualTripEntry.build(ManualTripForm(Platform.UBER, "0", now), "x", now) is ManualTripResult.Error)
        assertTrue(ManualTripEntry.build(ManualTripForm(Platform.UBER, "10", now, tripKm = "-1"), "x", now) is ManualTripResult.Error)
        assertTrue(ManualTripEntry.build(ManualTripForm(Platform.UBER, "10", now.plusSeconds(3600)), "x", now) is ManualTripResult.Error)
    }
}
