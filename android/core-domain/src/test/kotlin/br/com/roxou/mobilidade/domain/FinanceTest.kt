package br.com.roxou.mobilidade.domain

import br.com.roxou.mobilidade.domain.finance.DailyCascadeEngine
import br.com.roxou.mobilidade.domain.finance.FinanceEngine
import br.com.roxou.mobilidade.domain.fuel.FuelLog
import br.com.roxou.mobilidade.domain.model.EarningCategory
import br.com.roxou.mobilidade.domain.model.ExtraEarning
import br.com.roxou.mobilidade.domain.model.PaymentMethod
import br.com.roxou.mobilidade.domain.model.Platform
import br.com.roxou.mobilidade.domain.model.Trip
import br.com.roxou.mobilidade.domain.rental.RentalPayment
import br.com.roxou.mobilidade.domain.rental.RentalPaymentSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FinanceTest {
    private val at = br("2026-11-20T18:00")

    @Test fun `indicacao da 99 nao e receita de corrida`() {
        val s = FinanceEngine.summarize(
            trips = listOf(Trip("t", Platform.NINETY_NINE, at, 690_66)),
            extras = listOf(ExtraEarning("e", Platform.NINETY_NINE, EarningCategory.REFERRAL, at, 750_00)),
            rentalPayments = emptyList(), fuelLogs = emptyList(), expenses = emptyList(),
        )
        assertEquals(1_440_66L, s.grossRevenueCents)
        assertEquals(690_66L, s.rideRevenueCents)
        assertEquals(750_00L, s.extraRevenueCents)
    }

    @Test fun `parcela retida conta no faturamento mas nao no caixa`() {
        val s = FinanceEngine.summarize(
            trips = listOf(Trip("t", Platform.UBER, at, 1_134_40)),
            extras = emptyList(),
            rentalPayments = listOf(
                RentalPayment("r", LocalDate.of(2026, 11, 16), 989_00, RentalPaymentSource.UBER_WITHHOLDING, at, confirmed = true),
            ),
            fuelLogs = listOf(FuelLog("f", null, at, 40_000.0, 30.0, 180_00, true)),
            expenses = emptyList(),
        )
        assertEquals(1_134_40L, s.grossRevenueCents)
        assertEquals(1_134_40L - 989_00L - 180_00L, s.availableCashCents)
    }

    @Test fun `dinheiro em especie separado do app`() {
        val s = FinanceEngine.summarize(
            trips = listOf(
                Trip("a", Platform.NINETY_NINE, at, 429_17, PaymentMethod.CASH),
                Trip("b", Platform.NINETY_NINE, at, 671_05, PaymentMethod.APP),
            ),
            extras = emptyList(), rentalPayments = emptyList(), fuelLogs = emptyList(), expenses = emptyList(),
        )
        assertEquals(429_17L, s.cashReceivedCents)
        assertEquals(671_05L, s.appReceivedCents)
        assertEquals(1_100_22L, s.revenueByCategory[EarningCategory.NINETY_NINE_RIDE])
    }

    @Test fun `cascata cobre combustivel depois locacao depois lucro`() {
        val c = DailyCascadeEngine.compute(grossCents = 300_00, fuelTotalCents = 100_00, rentalTargetCents = 165_00)
        assertEquals(100_00L, c.fuelCoveredCents)
        assertEquals(165_00L, c.rentalCoveredCents)
        assertEquals(35_00L, c.freeProfitCents)
        assertTrue(c.isAllTargetMet)

        val partial = DailyCascadeEngine.compute(150_00, 100_00, 165_00)
        assertEquals(0L, partial.fuelRemainingCents)
        assertEquals(115_00L, partial.rentalRemainingCents)
        assertEquals(0L, partial.freeProfitCents)
    }

    @Test fun `meta diaria da locacao divide o pendente pelos dias restantes`() {
        assertEquals(164_15L, DailyCascadeEngine.rentalDailyTarget(1_149_00, 7)) // arredonda para cima
        assertEquals(0L, DailyCascadeEngine.rentalDailyTarget(0, 3))
        assertEquals(500_00L, DailyCascadeEngine.rentalDailyTarget(500_00, 1))
    }
}
