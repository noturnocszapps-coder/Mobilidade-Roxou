package br.com.roxou.mobilidade.domain

import br.com.roxou.mobilidade.domain.km.KmRecord
import br.com.roxou.mobilidade.domain.model.Platform
import br.com.roxou.mobilidade.domain.model.Trip
import br.com.roxou.mobilidade.domain.rental.RentalPayment
import br.com.roxou.mobilidade.domain.rental.RentalPaymentSource
import br.com.roxou.mobilidade.domain.rental.VehicleOwnership
import br.com.roxou.mobilidade.domain.summary.AppSettings
import br.com.roxou.mobilidade.domain.summary.Records
import br.com.roxou.mobilidade.domain.summary.SnapshotEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** Integração das regras de virada e de faixas, como o painel do turno vai usar. */
class SnapshotTest {
    private val settings = AppSettings()
    private var seq = 0
    private fun uber(at: Instant, confirmed: Boolean = true) = Trip("t${seq++}", Platform.UBER, at, 15_00, confirmed = confirmed)
    private fun many(n: Int, start: Instant) = (0 until n).map { uber(start.plusSeconds(it * 60L)) }

    @Test fun `corrida de segunda as 2h conta para a semana anterior e para o domingo`() {
        val trips = many(39, br("2026-11-27T16:00")) + uber(br("2026-11-30T02:00"))
        val s = SnapshotEngine.compute(settings, Records(trips), now = br("2026-11-30T03:00"))
        assertEquals(LocalDate.of(2026, 11, 23), s.rideWeek.id)
        assertEquals(LocalDate.of(2026, 11, 29), s.workday)
        assertEquals(40, s.rideCount.official)
        assertEquals(80_00L, s.tiers?.achievedDiscountCents)
        assertEquals(1, s.goals.days.last().done)
    }

    @Test fun `as 4h de segunda a contagem zera e o desconto vai para a parcela da semana nova`() {
        val trips = many(62, br("2026-11-27T16:00"))
        val s = SnapshotEngine.compute(settings, Records(trips), now = br("2026-11-30T04:00"))
        assertEquals(LocalDate.of(2026, 11, 30), s.rideWeek.id)
        assertEquals(0, s.rideCount.official)
        assertEquals(160_00L, s.installment?.discountAppliedCents)
        assertEquals(1_149_00L - 160_00L, s.installment?.dueCents)
        assertEquals(0L, s.installment?.discountSecuredForNextWeekCents)
        assertEquals(40, s.tiers?.ridesToNext)
    }

    @Test fun `correcao pelo painel da Uber muda a faixa da semana`() {
        val trips = many(58, br("2026-11-27T16:00"))
        val overrides = mapOf(LocalDate.of(2026, 11, 23) to 60)
        val now = br("2026-11-28T10:00")
        val s = SnapshotEngine.compute(settings, Records(trips, uberPanelOverrides = overrides), now)
        assertEquals(58, s.rideCount.countedByApp)
        assertEquals(60, s.rideCount.official)
        assertEquals(160_00L, s.tiers?.achievedDiscountCents)
        // Na semana seguinte, o desconto aplicado usa a contagem corrigida.
        val next = SnapshotEngine.compute(settings, Records(trips, uberPanelOverrides = overrides), br("2026-12-01T10:00"))
        assertEquals(160_00L, next.installment?.discountAppliedCents)
    }

    @Test fun `corrida nao confirmada e da 99 nao contam para as faixas da Localiza`() {
        val trips = listOf(
            uber(br("2026-11-27T16:00")),
            uber(br("2026-11-27T16:30"), confirmed = false),
            Trip("n", Platform.NINETY_NINE, br("2026-11-27T17:00"), 20_00),
        )
        val s = SnapshotEngine.compute(settings, Records(trips), br("2026-11-27T20:00"))
        assertEquals(1, s.rideCount.official)
        assertEquals(1, s.tripsTodayByPlatform[Platform.UBER])
        assertEquals(1, s.tripsTodayByPlatform[Platform.NINETY_NINE])
        assertEquals(35_00L + 15_00L, s.financeToday.grossRevenueCents)
    }

    @Test fun `franquia de km zera a 0h enquanto corridas seguem ate as 4h`() {
        val km = listOf(
            KmRecord("a", br("2026-11-29T23:30"), workKm = 1400.0),
            KmRecord("b", br("2026-11-30T01:00"), workKm = 20.0, privateKm = 5.0),
        )
        val trips = many(10, br("2026-11-29T20:00"))
        val s = SnapshotEngine.compute(settings, Records(trips, kmRecords = km), br("2026-11-30T02:00"))
        assertEquals(LocalDate.of(2026, 11, 30), s.kmWeek.id)
        assertEquals(25.0, s.km!!.totalKm, 0.001)
        assertEquals(10, s.rideCount.official) // corridas ainda na semana de 23/11
    }

    @Test fun `parcela pendente so cai com abatimento confirmado`() {
        val week = LocalDate.of(2026, 11, 23)
        val payments = listOf(
            RentalPayment("x", week, 1_000_00, RentalPaymentSource.UBER_WITHHOLDING, br("2026-11-23T05:00"), confirmed = false),
        )
        val s = SnapshotEngine.compute(settings, Records(rentalPayments = payments), br("2026-11-24T10:00"))
        assertEquals(1_149_00L, s.installment?.pendingCents)
        assertEquals(-1_000_00L, s.financeWeek.availableCashCents) // dinheiro retido saiu do caixa
    }

    @Test fun `carro proprio nao tem parcela nem faixas`() {
        val s = SnapshotEngine.compute(
            settings.copy(ownership = VehicleOwnership.OWN),
            Records(many(45, br("2026-11-27T16:00"))),
            br("2026-11-27T20:00"),
        )
        assertNull(s.installment)
        assertNull(s.tiers)
        assertNull(s.km)
        assertEquals(45, s.goals.done)
    }
}
