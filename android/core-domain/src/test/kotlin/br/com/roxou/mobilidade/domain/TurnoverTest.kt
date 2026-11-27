package br.com.roxou.mobilidade.domain

import br.com.roxou.mobilidade.domain.time.Turnover
import br.com.roxou.mobilidade.domain.time.TurnoverConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class TurnoverTest {
    private val t = Turnover()

    // Semana de referência: segunda 23/11/2026 a domingo 29/11/2026.

    @Test fun `dia de trabalho termina as 4h - corrida de sexta a 1h conta para sexta`() {
        // Sábado 28/11 01:00 ainda é o dia de trabalho de sexta 27/11.
        assertEquals(LocalDate.of(2026, 11, 27), t.workday(br("2026-11-28T01:00")))
        assertEquals(LocalDate.of(2026, 11, 27), t.workday(br("2026-11-28T03:59:59")))
        assertEquals(LocalDate.of(2026, 11, 28), t.workday(br("2026-11-28T04:00")))
        assertEquals(LocalDate.of(2026, 11, 27), t.workday(br("2026-11-27T15:30")))
    }

    @Test fun `madrugada de domingo para segunda ainda e domingo e semana anterior`() {
        val mondayEarly = br("2026-11-30T02:30") // segunda de madrugada
        assertEquals(LocalDate.of(2026, 11, 29), t.workday(mondayEarly))
        assertEquals(LocalDate.of(2026, 11, 23), t.rideWeek(mondayEarly).id)
    }

    @Test fun `semana de corridas vira na segunda as 4h`() {
        assertEquals(LocalDate.of(2026, 11, 23), t.rideWeek(br("2026-11-30T03:59:59")).id)
        assertEquals(LocalDate.of(2026, 11, 30), t.rideWeek(br("2026-11-30T04:00")).id)
        val w = t.rideWeek(br("2026-11-26T12:00"))
        assertEquals(br("2026-11-23T04:00"), w.start.toInstant())
        assertEquals(br("2026-11-30T04:00"), w.endExclusive.toInstant())
        assertTrue(br("2026-11-23T04:00") in w)
        assertFalse(br("2026-11-30T04:00") in w)
        assertFalse(br("2026-11-23T03:59") in w)
    }

    @Test fun `franquia de km vira na segunda as 0h`() {
        assertEquals(LocalDate.of(2026, 11, 23), t.kmWeek(br("2026-11-29T23:59:59")).id)
        assertEquals(LocalDate.of(2026, 11, 30), t.kmWeek(br("2026-11-30T00:00")).id)
        // Entre 00h e 04h de segunda: franquia já é da semana nova, corridas ainda da anterior.
        val between = br("2026-11-30T02:00")
        assertEquals(LocalDate.of(2026, 11, 30), t.kmWeek(between).id)
        assertEquals(LocalDate.of(2026, 11, 23), t.rideWeek(between).id)
    }

    @Test fun `viradas usam horario de Brasilia mesmo vindo de UTC`() {
        // Segunda 30/11 06:59 UTC = 03:59 em Brasília (UTC-3): ainda semana anterior.
        val utc = LocalDate.of(2026, 11, 30).atTime(6, 59).toInstant(ZoneOffset.UTC)
        assertEquals(LocalDate.of(2026, 11, 23), t.rideWeek(utc).id)
        val utc2 = LocalDate.of(2026, 11, 30).atTime(7, 0).toInstant(ZoneOffset.UTC)
        assertEquals(LocalDate.of(2026, 11, 30), t.rideWeek(utc2).id)
    }

    @Test fun `semana de corridas cobre os sete dias de trabalho de segunda a domingo`() {
        val days = t.workdaysOf(t.rideWeek(br("2026-11-25T10:00")))
        assertEquals(7, days.size)
        assertEquals(LocalDate.of(2026, 11, 23), days.first())
        assertEquals(LocalDate.of(2026, 11, 29), days.last())
        assertEquals(DayOfWeek.SUNDAY, days.last().dayOfWeek)
    }

    @Test fun `todo dia de trabalho de uma corrida cai dentro da semana dela`() {
        var i = br("2026-11-16T00:00")
        val end = br("2026-12-08T00:00")
        while (i.isBefore(end)) {
            val week = t.rideWeek(i)
            assertTrue(t.workday(i) in t.workdaysOf(week))
            i = i.plusSeconds(15 * 60)
        }
    }

    @Test fun `horarios de virada sao configuraveis`() {
        val custom = Turnover(
            TurnoverConfig(
                rideWeekResetDay = DayOfWeek.SUNDAY,
                rideWeekResetTime = LocalTime.of(5, 0),
                workdayEndTime = LocalTime.of(5, 0),
                kmResetDay = DayOfWeek.TUESDAY,
            ),
        )
        assertEquals(LocalDate.of(2026, 11, 22), custom.rideWeek(br("2026-11-29T04:59")).id)
        assertEquals(LocalDate.of(2026, 11, 29), custom.rideWeek(br("2026-11-29T05:00")).id)
        assertEquals(LocalDate.of(2026, 11, 27), custom.workday(br("2026-11-28T04:30")))
        assertEquals(LocalDate.of(2026, 11, 24), custom.kmWeek(br("2026-11-30T12:00")).id)
    }

    @Test fun `semana anterior e seguinte encadeiam sem buraco`() {
        val w = t.rideWeek(br("2026-11-26T12:00"))
        assertEquals(w.start, w.previous().endExclusive)
        assertEquals(w.endExclusive, w.next().start)
        assertEquals(w, t.rideWeekStartingOn(LocalDate.of(2026, 11, 23)))
    }
}
