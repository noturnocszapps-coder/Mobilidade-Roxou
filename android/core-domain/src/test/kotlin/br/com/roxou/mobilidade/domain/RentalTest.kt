package br.com.roxou.mobilidade.domain

import br.com.roxou.mobilidade.domain.rental.RentalEngine
import br.com.roxou.mobilidade.domain.rental.RentalPayment
import br.com.roxou.mobilidade.domain.rental.RentalPaymentSource
import br.com.roxou.mobilidade.domain.rental.RentalPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RentalTest {
    private val plan = RentalPlan.LOCALIZA_ZARP
    private val week = LocalDate.of(2026, 11, 23)

    private fun pay(cents: Long, confirmed: Boolean, w: LocalDate = week) =
        RentalPayment("p$cents$confirmed", w, cents, RentalPaymentSource.UBER_WITHHOLDING, br("2026-11-23T05:00"), confirmed)

    @Test fun `desconto vem da semana anterior e nao da atual`() {
        // Semana anterior: 67 corridas (faixa 60 = R$ 160). Semana atual: 82 (garante R$ 250 para a próxima).
        val s = RentalEngine.installment(plan, week, ridesPreviousWeek = 67, ridesThisWeek = 82, payments = emptyList())
        assertEquals(160_00L, s.discountAppliedCents)
        assertEquals(1_149_00L - 160_00L, s.dueCents)
        assertEquals(250_00L, s.discountSecuredForNextWeekCents)
        assertEquals(s.dueCents, s.pendingCents)
    }

    @Test fun `so abatimento confirmado conta como pago`() {
        val s = RentalEngine.installment(plan, week, 0, 0, listOf(pay(500_00, true), pay(649_00, false)))
        assertEquals(500_00L, s.confirmedPaidCents)
        assertEquals(649_00L, s.unconfirmedCents)
        assertEquals(649_00L, s.pendingCents)
        assertFalse(s.isPaid)
    }

    @Test fun `parcela quitada e pagamento de outra semana nao conta`() {
        val s = RentalEngine.installment(
            plan, week, 40, 0,
            listOf(pay(1_069_00, true), pay(1_149_00, true, w = week.minusWeeks(1))),
        )
        assertEquals(1_069_00L, s.dueCents)
        assertEquals(0L, s.pendingCents)
        assertTrue(s.isPaid)
        assertEquals(0L, s.overpaidCents)
    }

    @Test fun `pagamento acima do devido aparece como excedente`() {
        val s = RentalEngine.installment(plan, week, 80, 0, listOf(pay(1_149_00, true)))
        assertEquals(250_00L, s.overpaidCents)
    }

    @Test fun `locadora sem faixas cobra a parcela cheia`() {
        val s = RentalEngine.installment(RentalPlan.MOVIDA_FX, week, 90, 90, emptyList())
        assertEquals(878_98L, s.dueCents)
        assertEquals(0L, s.discountSecuredForNextWeekCents)
    }
}
